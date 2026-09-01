import { Component, DestroyRef, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { Session } from './auth/session';

/**
 * Composant racine.
 *
 * <p>Il ne rend qu'un `router-outlet`, mais il a une responsabilité : **démarrer le flux
 * OIDC dès le lancement de l'application**. La découverte Keycloak est un appel réseau ;
 * la lancer seulement quand un écran en a besoin ouvre une fenêtre pendant laquelle
 * « Se connecter » ne fait rien.
 */
@Component({
  selector: 'sd-root',
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export class App {
  private readonly session = inject(Session);

  constructor() {
    this.session.demarrer();

    // Au retour sur l'onglet, on redemande au portail si la session vit encore. C'est ce qui
    // rend visible une déconnexion faite depuis une AUTRE application du realm — sinon
    // l'application affiche une session fermée pendant cinq minutes.
    const surRetour = () => {
      if (document.visibilityState === 'visible') {
        this.session.revaliderAupresDuPortail();
      }
    };
    document.addEventListener('visibilitychange', surRetour);
    inject(DestroyRef).onDestroy(() => document.removeEventListener('visibilitychange', surRetour));
  }
}
