import { Component, computed, input } from '@angular/core';

import { StatutTicket } from '../ticket.model';

interface TraitementStatut {
  libelle: string;
  glyphe: string;
  /** Lu par les lecteurs d'écran à la place du glyphe décoratif. */
  description: string;
  barre: boolean;
}

const TRAITEMENTS: Record<StatutTicket, TraitementStatut> = {
  OUVERT: { libelle: 'OUVERT', glyphe: '●', description: 'ticket ouvert', barre: false },
  EN_COURS: { libelle: 'EN_COURS', glyphe: '◑', description: 'ticket en cours', barre: false },
  RESOLU: { libelle: 'RESOLU', glyphe: '✓', description: 'ticket résolu', barre: false },
  FERME: { libelle: 'FERME', glyphe: '✕', description: 'ticket fermé', barre: true },
};

/**
 * Badge de statut.
 *
 * Quatre traitements simultanés — couleur, forme du cadre, glyphe, libellé — parce que
 * la couleur seule ne se lit ni en niveaux de gris ni en vision dichromate. Le libellé
 * de FERME est en plus barré. Ne jamais réduire ce composant à une pastille colorée.
 */
@Component({
  selector: 'sd-badge-statut',
  templateUrl: './badge-statut.html',
  styleUrl: './badge-statut.scss',
})
export class BadgeStatut {
  readonly statut = input.required<StatutTicket>();

  protected readonly traitement = computed(() => TRAITEMENTS[this.statut()]);
}
