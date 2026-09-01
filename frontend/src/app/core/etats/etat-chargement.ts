import { Component, input } from '@angular/core';

/**
 * Squelette de tableau, calé sur la hauteur de ligne réelle (36 px) pour éviter le saut
 * de mise en page à l'arrivée des données.
 */
@Component({
  selector: 'sd-etat-chargement',
  template: `
    <div class="squelette" role="status" aria-live="polite">
      <span class="sd-hors-ecran">Chargement en cours</span>
      @for (ligne of lignes(); track $index) {
        <div class="squelette__ligne" [style.animation-delay]="$index * 100 + 'ms'">
          <span class="squelette__cellule squelette__cellule--courte"></span>
          <span class="squelette__cellule"></span>
        </div>
      }
    </div>
  `,
  styleUrl: './etat-chargement.scss',
})
export class EtatChargement {
  readonly nombreLignes = input(5);

  protected lignes() {
    return Array.from({ length: this.nombreLignes() });
  }
}
