import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Session terminée (écran 10).
 *
 * Traitement inverse du 403 : fond sombre, pleine page, aucun en-tête, aucune identité,
 * une seule action. Le 403 conserve au contraire la coque de l'application connectée —
 * on ne doit pas confondre « vous n'êtes plus connecté » et « ce ticket n'est pas le vôtre ».
 */
@Component({
  selector: 'sd-session-terminee',
  imports: [RouterLink],
  template: `
    <div class="fin">
      <div class="fin__carte">
        <div class="fin__marque">SUPPORTDESK</div>
        <span class="fin__badge">SESSION FERMÉE</span>
        <h1 class="fin__titre">Vous êtes déconnecté</h1>
        <p class="fin__texte">
          Votre session a pris fin. Reconnectez-vous pour retrouver vos tickets.
        </p>
        <a routerLink="/connexion" class="fin__bouton">Se reconnecter</a>
      </div>
    </div>
  `,
  styleUrl: './session-terminee.scss',
})
export class SessionTerminee {}
