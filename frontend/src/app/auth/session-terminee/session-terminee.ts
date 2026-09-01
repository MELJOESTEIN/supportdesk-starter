import { Component, inject } from '@angular/core';

import { Session } from '../session';

/**
 * Session terminée (écran 10).
 *
 * Traitement inverse du 403 : fond sombre, pleine page, aucun en-tête, aucune identité,
 * une seule action. Le 403 conserve au contraire la coque de l'application connectée —
 * on ne doit pas confondre « vous n'êtes plus connecté » et « ce ticket n'est pas le vôtre ».
 */
@Component({
  selector: 'sd-session-terminee',
  template: `
    <div class="fin">
      <div class="fin__carte">
        <div class="fin__marque">SUPPORTDESK</div>
        <span class="fin__badge">SESSION FERMÉE</span>
        <h1 class="fin__titre">Vous êtes déconnecté</h1>
        <p class="fin__texte">
          <!-- « vos tickets » ne veut rien dire pour un agent, qui a une file et des
               assignations. Le même écran sert les deux rôles : il doit parler des deux. -->
          Votre session a pris fin. Reconnectez-vous pour retrouver votre espace de travail.
        </p>
        <button type="button" class="fin__bouton" [disabled]="!session.pret()" (click)="reconnecter()">
          {{ session.pret() ? 'Se reconnecter' : 'Préparation…' }}
        </button>
      </div>
    </div>
  `,
  styleUrl: './session-terminee.scss',
})
export class SessionTerminee {
  protected readonly session = inject(Session);

  protected reconnecter(): void {
    this.session.connecter();
  }
}
