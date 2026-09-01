import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { Session, UTILISATEUR_CLIENT } from '../session';

/**
 * Retour de connexion (écran 09).
 *
 * Écran transitoire : le temps d'échanger le code d'autorisation contre un jeton. Au lot 2
 * la connexion est simulée ; au lot 4 c'est ici que la bibliothèque OIDC termine le flux
 * PKCE. La variante d'échec existe dès maintenant parce qu'elle existera en production.
 */
@Component({
  selector: 'sd-retour-connexion',
  template: `
    <div class="retour">
      <div class="retour__carte">
        <div class="retour__marque">SUPPORTDESK</div>

        @if (echec()) {
          <h1 class="retour__titre">La connexion n'a pas abouti</h1>
          <p class="retour__texte">Vous pouvez réessayer maintenant.</p>
          <button type="button" class="retour__bouton" (click)="reessayer()">Réessayer</button>
        } @else {
          <div class="retour__points" aria-hidden="true">
            <span></span><span></span><span></span>
          </div>
          <p class="retour__etat" role="status">Connexion en cours…</p>
          <p class="retour__mention">Ne fermez pas cette fenêtre.</p>
        }
      </div>
    </div>
  `,
  styleUrl: './retour-connexion.scss',
})
export class RetourConnexion {
  private readonly session = inject(Session);
  private readonly router = inject(Router);

  protected readonly echec = signal(false);

  constructor() {
    // Lot 2 : connexion simulée. Lot 4 : `checkAuth()` de la bibliothèque OIDC.
    setTimeout(() => {
      this.session.simulerConnexion(UTILISATEUR_CLIENT);
      void this.router.navigate(['/mes-tickets']);
    }, 900);
  }

  protected reessayer(): void {
    this.echec.set(false);
    void this.router.navigate(['/connexion']);
  }
}
