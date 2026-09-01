import { Component, inject } from '@angular/core';

import { Session } from '../session';

/**
 * Accueil non connecté (écran 08).
 *
 * Aucun menu, aucune donnée, et surtout **aucun champ de saisie** : l'identification a lieu
 * sur Keycloak. L'application ne voit jamais d'identifiant ni de mot de passe — c'est tout
 * l'intérêt d'un fournisseur d'identité, et ça se voit à l'écran.
 *
 * Le bouton reste désactivé tant que la découverte OIDC n'est pas chargée : sans cela, un
 * clic trop rapide ne déclenche aucune redirection, sans le moindre message.
 */
@Component({
  selector: 'sd-accueil',
  template: `
    <div class="accueil">
      <div class="accueil__carte">
        <div class="accueil__marque">SUPPORTDESK</div>
        <h1 class="sd-hors-ecran">Connexion à SupportDesk</h1>
        <p class="accueil__texte">
          L'espace de suivi des demandes de support. Vos tickets, leurs réponses et leur
          avancement, au même endroit.
        </p>

        <button
          type="button"
          class="accueil__bouton"
          [disabled]="!session.pret() || session.erreur() !== null"
          (click)="connecter()"
        >
          {{ session.pret() ? 'Se connecter' : 'Préparation de la connexion…' }}
        </button>

        @if (session.erreur(); as erreur) {
          <p class="accueil__erreur" role="alert">
            {{ erreur }} Réessayez dans un instant, ou prévenez votre administrateur.
          </p>
        } @else {
          <p class="accueil__mention">
            Vous serez redirigé vers le portail d'identité de votre organisation.
          </p>
        }
      </div>
    </div>
  `,
  styleUrl: './accueil.scss',
})
export class Accueil {
  protected readonly session = inject(Session);

  /** Redirige vers Keycloak. L'application ne voit jamais d'identifiant ni de mot de passe. */
  protected connecter(): void {
    this.session.connecter();
  }
}
