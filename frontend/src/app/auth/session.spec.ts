import { TestBed } from '@angular/core/testing';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';

import { Session } from './session';

/**
 * Le démarrage du flux OIDC.
 *
 * Ces tests existent à cause d'un vrai défaut : `authorize()` ne fait **rien** tant que la
 * découverte OIDC n'est pas chargée, et il ne le signale pas. Un clic pendant cette fenêtre
 * ne déclenchait aucune redirection — l'utilisateur restait sur l'accueil sans message et
 * croyait à une boucle.
 */
describe('Session — démarrage OIDC', () => {
  function preparer(oidc: Partial<OidcSecurityService>): Session {
    TestBed.configureTestingModule({
      providers: [{ provide: OidcSecurityService, useValue: oidc }],
    });
    return TestBed.inject(Session);
  }

  it("n'est pas prête avant que la découverte soit chargée", () => {
    const session = preparer({
      checkAuth: vi.fn(() => of({ isAuthenticated: false })) as never,
    });

    // Avant `demarrer()`, rien n'a été chargé : le bouton doit rester désactivé.
    expect(session.pret()).toBe(false);
  });

  it('devient prête après le démarrage, même sans session existante', () => {
    const session = preparer({
      checkAuth: vi.fn(() => of({ isAuthenticated: false })) as never,
    });

    session.demarrer();

    expect(session.pret()).toBe(true);
    expect(session.connecte()).toBe(false);
    expect(session.erreur()).toBeNull();
  });

  it('reconstitue l\'utilisateur depuis les claims du jeton', () => {
    const session = preparer({
      checkAuth: vi.fn(() => of({ isAuthenticated: true })) as never,
      getPayloadFromAccessToken: vi.fn(() =>
        of({
          preferred_username: 'alice',
          name: 'Alice Durand',
          crm_client_ref: 'CLI-0001',
          realm_access: { roles: ['CLIENT', 'offline_access'] },
        }),
      ) as never,
    });

    session.demarrer();

    const utilisateur = session.utilisateur();
    expect(utilisateur?.username).toBe('alice');
    expect(utilisateur?.crmClientRef).toBe('CLI-0001');
    // Les rôles techniques de Keycloak ne sont pas retenus.
    expect(utilisateur?.roles).toEqual(['CLIENT']);
    expect(session.estAgent()).toBe(false);
  });

  it("reste prête et le signale si le fournisseur d'identité est injoignable", () => {
    const session = preparer({
      checkAuth: vi.fn(() => throwError(() => new Error('ECONNREFUSED'))) as never,
    });

    session.demarrer();

    // Sans cette gestion, `pret()` resterait faux pour toujours et le bouton
    // « Se connecter » serait définitivement grisé, sans explication.
    expect(session.pret()).toBe(true);
    expect(session.erreur()).toContain('injoignable');
  });

  it('déclenche bien la redirection vers Keycloak', () => {
    const authorize = vi.fn();
    const session = preparer({
      checkAuth: vi.fn(() => of({ isAuthenticated: false })) as never,
      authorize: authorize as never,
    });

    session.demarrer();
    session.connecter();

    expect(authorize).toHaveBeenCalledOnce();
  });
});
