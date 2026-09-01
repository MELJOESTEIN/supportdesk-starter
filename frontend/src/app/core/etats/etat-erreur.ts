import { Component, input, output } from '@angular/core';

/**
 * Erreur serveur.
 *
 * Trois choses que le code généré oublie systématiquement : dire que la saisie en cours
 * est conservée, donner une référence citable, et proposer de réessayer sans recharger.
 */
@Component({
  selector: 'sd-etat-erreur',
  template: `
    <div class="erreur" role="alert">
      <div class="erreur__icone" aria-hidden="true">!</div>
      <h4 class="erreur__titre">{{ titre() }}</h4>
      <p class="erreur__explication">{{ explication() }}</p>
      <div class="erreur__actions">
        <button type="button" class="erreur__bouton erreur__bouton--primaire" (click)="reessayer.emit()">
          Réessayer
        </button>
      </div>
      @if (reference()) {
        <p class="erreur__reference">
          Référence : {{ reference() }}<br />À citer si vous signalez l'incident.
        </p>
      }
    </div>
  `,
  styleUrl: './etat-erreur.scss',
})
export class EtatErreur {
  readonly titre = input("La liste n'a pas pu être chargée");
  readonly explication = input(
    "Le serveur n'a pas répondu. Vos filtres et votre saisie en cours sont conservés.",
  );
  readonly reference = input<string | null>(null);

  readonly reessayer = output<void>();
}
