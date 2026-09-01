import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { catchError, of } from 'rxjs';

import { RoleUtilisateur, Utilisateur } from './auth.model';

interface ClaimsKeycloak {
  preferred_username?: string;
  name?: string;
  email?: string;
  crm_client_ref?: string;
  realm_access?: { roles?: string[] };
}

const ROLES_CONNUS: RoleUtilisateur[] = ['CLIENT', 'AGENT', 'ADMIN'];

/**
 * Session de l'utilisateur courant, alimentée par les claims du jeton.
 *
 * <p>Ce que porte cet objet sert à décider ce qu'on **affiche**. Ce qu'un utilisateur a le
 * droit d'**obtenir** est décidé par le backend, à partir du même jeton. Modifier ce signal
 * dans la console du navigateur fait apparaître des boutons, et rien de plus : les appels
 * derrière eux repartiront en 403.
 *
 * <h2>Pourquoi `demarrer()` est appelé par le composant racine</h2>
 *
 * <p>`checkAuth()` fait un appel réseau : il charge la découverte OIDC de Keycloak
 * (`.well-known/openid-configuration`) avant de pouvoir quoi que ce soit. Tant qu'elle
 * n'est pas chargée, **`authorize()` ne fait rien — silencieusement**.
 *
 * <p>Si on ne lance ce chargement qu'au moment où un écran injecte ce service, un clic
 * rapide sur « Se connecter » tombe pendant la course et ne déclenche aucune redirection.
 * L'utilisateur reste sur la page d'accueil sans le moindre message, reclique, et croit à
 * une boucle. D'où deux mesures : le chargement démarre avec l'application, et le bouton
 * reste désactivé tant que `pret()` est faux.
 */
@Injectable({ providedIn: 'root' })
export class Session {
  private readonly oidc = inject(OidcSecurityService);
  private readonly router = inject(Router);

  private readonly _utilisateur = signal<Utilisateur | null>(null);
  private readonly _pret = signal(false);
  private readonly _erreur = signal<string | null>(null);

  private declarerPrete!: () => void;

  /**
   * Résolue quand la découverte OIDC a répondu — succès, échec ou absence de session.
   *
   * <p>Les gardes de route l'attendent. Sans cela, un accès direct à une URL protégée
   * (rechargement, favori, lien collé) est arbitré **avant** que `checkAuth()` ait répondu :
   * `connecte()` vaut encore `false`, et l'utilisateur pourtant connecté est renvoyé à
   * l'accueil. Le bogue ne se voit pas en naviguant dans l'application, seulement en
   * arrivant de l'extérieur — c'est ce qui le rend coûteux à trouver.
   */
  private readonly attenteDecouverte = new Promise<void>((resoudre) => {
    this.declarerPrete = resoudre;
  });

  /** L'utilisateur connecté, ou `null`. */
  readonly utilisateur = this._utilisateur.asReadonly();

  /** La découverte OIDC est chargée : on peut lancer une connexion. */
  readonly pret = this._pret.asReadonly();

  /** Message d'erreur si le fournisseur d'identité est injoignable. */
  readonly erreur = this._erreur.asReadonly();

  readonly connecte = computed(() => this._utilisateur() !== null);
  readonly estAgent = computed(() => this.aLeRole('AGENT') || this.aLeRole('ADMIN'));

  /**
   * Démarre le flux OIDC. Appelé une seule fois, par le composant racine.
   *
   * <p>Termine l'échange du code d'autorisation au retour de Keycloak, et restaure une
   * session existante au rechargement de la page.
   */
  demarrer(): void {
    this.oidc
      .checkAuth()
      .pipe(
        catchError(() => {
          // Keycloak injoignable : l'application doit rester utilisable et le dire,
          // plutôt que d'attendre indéfiniment un bouton qui ne répondra jamais.
          this._erreur.set("Le portail d'identité est injoignable.");
          return of({ isAuthenticated: false } as { isAuthenticated: boolean });
        }),
      )
      .subscribe(({ isAuthenticated }) => {
        if (!isAuthenticated) {
          this._utilisateur.set(null);
          this.marquerPrete();
          return;
        }
        this.oidc.getPayloadFromAccessToken().subscribe((claims) => {
          this._utilisateur.set(this.depuisClaims(claims as ClaimsKeycloak));
          this.marquerPrete();
        });
      });
  }

  /** À attendre avant de décider d'un accès. Voir {@link attenteDecouverte}. */
  quandPrete(): Promise<void> {
    return this.attenteDecouverte;
  }

  aLeRole(role: RoleUtilisateur): boolean {
    return this._utilisateur()?.roles.includes(role) ?? false;
  }

  /** Redirige vers Keycloak. Sans effet tant que `pret()` est faux — d'où le bouton désactivé. */
  connecter(): void {
    this.oidc.authorize();
  }

  deconnecter(): void {
    // Déconnexion côté fournisseur d'identité, pas seulement locale : effacer le jeton
    // du navigateur laisserait la session Keycloak ouverte, et la reconnexion serait
    // immédiate et silencieuse.
    this.oidc.logoff().subscribe();
    this._utilisateur.set(null);
  }

  /**
   * Redemande au portail d'identité si la session existe encore.
   *
   * <p><b>Le problème.</b> Une déconnexion faite depuis une autre application du realm —
   * l'intranet du J2, par exemple — détruit la session Keycloak sur-le-champ. Mais le jeton
   * d'accès rangé dans ce navigateur reste **valide jusqu'à son expiration**, cinq minutes
   * ici. L'application continue donc d'afficher une session déjà fermée, et rien dans le
   * jeton ne permet de s'en apercevoir : il est signé, daté, et personne ne peut le
   * « dé-émettre ». C'est la nature même d'un jeton porteur.
   *
   * <p><b>Le signal.</b> Le jeton de rafraîchissement, lui, est invalidé immédiatement. On
   * tente donc un rafraîchissement : s'il échoue, la session est morte ailleurs.
   *
   * <p>Déclenché quand l'onglet redevient visible — le geste exact de quelqu'un qui revient
   * d'une autre application. Sans cela, il faudrait attendre l'expiration du jeton, et la
   * démonstration du J2 serait inutilisable en salle.
   */
  revaliderAupresDuPortail(): void {
    if (!this.connecte()) {
      return;
    }
    this.oidc
      .forceRefreshSession()
      .pipe(catchError(() => of({ isAuthenticated: false } as { isAuthenticated: boolean })))
      .subscribe(({ isAuthenticated }) => {
        if (!isAuthenticated) {
          this.terminerLocalement();
          void this.router.navigate(['/deconnexion']);
        }
      });
  }

  /**
   * Oublie la session **sans** passer par le fournisseur d'identité.
   *
   * <p>Pour un 401 : le jeton est déjà refusé, il n'y a rien à fermer chez Keycloak. Appeler
   * `logoff()` ici envoie le navigateur sur le point de terminaison de fin de session
   * <b>sans `id_token_hint`</b> — Keycloak répond alors par une page d'erreur nue, en
   * anglais, sans retour possible. L'utilisateur est sorti de l'application par une porte
   * qui n'en est pas une.
   */
  terminerLocalement(): void {
    this.oidc.logoffLocal();
    this._utilisateur.set(null);
  }

  private marquerPrete(): void {
    this._pret.set(true);
    this.declarerPrete();
  }

  private depuisClaims(claims: ClaimsKeycloak | null): Utilisateur | null {
    if (!claims?.preferred_username) {
      return null;
    }
    return {
      username: claims.preferred_username,
      nomComplet: claims.name ?? claims.preferred_username,
      email: claims.email ?? null,
      roles: (claims.realm_access?.roles ?? []).filter((r): r is RoleUtilisateur =>
        ROLES_CONNUS.includes(r as RoleUtilisateur),
      ),
      crmClientRef: claims.crm_client_ref ?? null,
    };
  }
}
