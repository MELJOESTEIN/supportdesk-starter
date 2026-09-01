import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { provideRouter } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';

import { routes } from '../app.routes';
import { Utilisateur } from './auth.model';
import { gardeAnonyme, gardeConnecte, gardeRole } from './garde-role';
import { Session } from './session';

const CLIENT: Utilisateur = {
  username: 'alice',
  nomComplet: 'Alice Durand',
  email: null,
  roles: ['CLIENT'],
  crmClientRef: 'CLI-0001',
};

const AGENT: Utilisateur = {
  username: 'bob',
  nomComplet: 'Bob Lefevre',
  email: null,
  roles: ['AGENT'],
  crmClientRef: null,
};

/** Session factice : les tests de garde n'ont pas à parler à Keycloak. */
class SessionFactice {
  utilisateur = vi.fn<() => Utilisateur | null>(() => null);
  connecte = () => this.utilisateur() !== null;
  estAgent = () => this.aLeRole('AGENT') || this.aLeRole('ADMIN');
  aLeRole = (role: string) => this.utilisateur()?.roles.includes(role as never) ?? false;
  quandPrete = () => Promise.resolve();
}

type Garde = () => Promise<boolean | UrlTree>;

/**
 * Ce que les gardes font — et surtout ce qu'elles ne font pas.
 *
 * Le dernier test est le plus important de ce fichier : il documente noir sur blanc qu'une
 * garde franchie ne donne accès à aucune donnée.
 */
describe('Gardes de route', () => {
  function preparer(utilisateur: Utilisateur | null) {
    const session = new SessionFactice();
    session.utilisateur = vi.fn(() => utilisateur);

    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: Session, useValue: session }],
    });
    return TestBed.inject(Router);
  }

  it('laisse passer un utilisateur connecté', async () => {
    preparer(CLIENT);
    const resultat = await TestBed.runInInjectionContext(() => (gardeConnecte() as Garde)());

    expect(resultat).toBe(true);
  });

  it("renvoie à l'accueil un utilisateur non connecté", async () => {
    preparer(null);
    const resultat = await TestBed.runInInjectionContext(() => (gardeConnecte() as Garde)());

    expect(resultat).toBeInstanceOf(UrlTree);
    expect(String(resultat)).toBe('/accueil');
  });

  it('ouvre la zone agent à un agent', async () => {
    preparer(AGENT);
    const resultat = await TestBed.runInInjectionContext(() =>
      (gardeRole('AGENT', 'ADMIN') as Garde)(),
    );

    expect(resultat).toBe(true);
  });

  it('ferme la zone agent à un client', async () => {
    preparer(CLIENT);
    const resultat = await TestBed.runInInjectionContext(() =>
      (gardeRole('AGENT', 'ADMIN') as Garde)(),
    );

    // Le paramètre déclenche l'explication sur l'écran d'arrivée : le renvoi était muet,
    // alors que le 403 d'un ticket prend trois paragraphes pour dire ce qui se passe.
    expect(String(resultat)).toBe('/mes-tickets?acces=reserve');
  });

  it("attend la fin de la découverte OIDC avant d'arbitrer", async () => {
    // Le cas qui n'arrive qu'au rechargement d'une URL protégée : la garde est appelée
    // pendant que checkAuth() est encore en vol. Une garde qui répond tout de suite renvoie
    // à l'accueil quelqu'un de parfaitement connecté.
    const session = new SessionFactice();
    session.utilisateur = vi.fn(() => null);
    let liberer!: () => void;
    session.quandPrete = () =>
      new Promise<void>((resoudre) => {
        liberer = () => {
          session.utilisateur = vi.fn(() => CLIENT);
          resoudre();
        };
      });

    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: Session, useValue: session }],
    });

    const enCours = TestBed.runInInjectionContext(() => (gardeConnecte() as Garde)());
    liberer();

    expect(await enCours).toBe(true);
  });

  it("renvoie chez lui quelqu'un de connecté qui ouvre l'écran de connexion", async () => {
    // `/accueil` affichait « Se connecter » à quelqu'un de déjà connecté. Deux écrans
    // donnaient deux réponses différentes à la même question, et c'est ce qui a rendu
    // illisible le diagnostic de la déconnexion unique.
    preparer(CLIENT);
    const resultat = await TestBed.runInInjectionContext(() => (gardeAnonyme() as Garde)());

    expect(String(resultat)).toBe('/mes-tickets');
  });

  it("laisse l'écran de connexion à qui n'est pas connecté", async () => {
    preparer(null);
    const resultat = await TestBed.runInInjectionContext(() => (gardeAnonyme() as Garde)());

    expect(resultat).toBe(true);
  });

  it("l'espace client est bien gardé — le câblage, pas seulement la fonction", () => {
    // Le test qui manquait. Les autres appellent la garde à la main : ils passaient tous
    // alors que `gardeConnecte` était importée dans app.routes.ts et jamais appliquée.
    // Déconnecté, /mes-tickets s'ouvrait, appelait l'API, recevait un 401, et l'intercepteur
    // expédiait l'utilisateur sur une page d'erreur Keycloak sans retour.
    const espaceClient = routes.find(
      (route) => route.path === '' && route.children?.some((e) => e.path === 'mes-tickets'),
    );

    expect(espaceClient, "l'espace client a disparu du routage").toBeDefined();
    expect(espaceClient?.canActivate ?? []).toHaveLength(1);
  });

  it("ne protège aucune donnée : c'est de l'affichage", () => {
    // Une garde décide ce qu'on montre. Elle ne fait aucun appel réseau, ne consulte
    // aucune donnée, et ne peut donc rien empêcher de fuir. Un client qui force la route
    // /agent verra la coque du back-office — et des écrans vides, parce que l'API répond
    // 403 (voir AutorisationFonctionsTests, côté backend).
    preparer(CLIENT);
    const garde = gardeRole('AGENT');

    expect(typeof garde).toBe('function');
  });
});
