import { Component, input, output } from '@angular/core';

/** Liste vide : dire pourquoi elle est vide, et proposer la sortie. */
@Component({
  selector: 'sd-etat-vide',
  template: `
    <div class="vide">
      <div class="vide__icone" aria-hidden="true">⌕</div>
      <h4 class="vide__titre">{{ titre() }}</h4>
      <p class="vide__explication">{{ explication() }}</p>
      @if (actionPrincipale()) {
        <div class="vide__actions">
          <button type="button" class="vide__bouton vide__bouton--primaire" (click)="principale.emit()">
            {{ actionPrincipale() }}
          </button>
          @if (actionSecondaire()) {
            <button type="button" class="vide__bouton" (click)="secondaire.emit()">
              {{ actionSecondaire() }}
            </button>
          }
        </div>
      }
    </div>
  `,
  styleUrl: './etat-vide.scss',
})
export class EtatVide {
  readonly titre = input('Aucun ticket ne correspond');
  readonly explication = input('');
  readonly actionPrincipale = input<string | null>(null);
  readonly actionSecondaire = input<string | null>(null);

  readonly principale = output<void>();
  readonly secondaire = output<void>();
}
