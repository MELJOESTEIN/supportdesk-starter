import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { EtatChargement } from '../../core/etats/etat-chargement';
import { EtatErreur } from '../../core/etats/etat-erreur';
import { DateLonguePipe } from '../../core/format/date-longue-pipe';
import { StatutTicket } from '../../ticket/ticket.model';
import { TableauDeBordService } from '../tableau-de-bord.service';

const GLYPHES: Record<StatutTicket, string> = {
  OUVERT: '●',
  EN_COURS: '◑',
  RESOLU: '✓',
  FERME: '✕',
};

const LIBELLES: Record<StatutTicket, string> = {
  OUVERT: 'Ouvert',
  EN_COURS: 'En cours',
  RESOLU: 'Résolu',
  FERME: 'Fermé',
};

/** Tableau de bord agent (écran 04) : six indicateurs, activité 14 jours, répartition. */
@Component({
  selector: 'sd-tableau-de-bord-page',
  imports: [RouterLink, EtatChargement, EtatErreur, DateLonguePipe],
  templateUrl: './tableau-de-bord-page.html',
  styleUrl: './tableau-de-bord-page.scss',
})
export class TableauDeBordPage {
  private readonly service = inject(TableauDeBordService);

  protected readonly donnees = this.service.charger();

  protected readonly maximum = computed(() => {
    const d = this.donnees.value()?.tableauDeBord;
    if (!d) {
      return 1;
    }
    return Math.max(...d.activite.flatMap((j) => [j.crees, j.resolus]), 1);
  });

  protected hauteur(valeur: number): string {
    return `${Math.round((valeur / this.maximum()) * 100)}%`;
  }

  protected part(nombre: number): string {
    const total = this.donnees.value()?.tableauDeBord.totalPeriode ?? 1;
    return `${Math.round((nombre / total) * 100)}%`;
  }

  protected glyphe(statut: StatutTicket): string {
    return GLYPHES[statut];
  }

  protected libelle(statut: StatutTicket): string {
    return LIBELLES[statut];
  }

  protected jourCourt(jour: string): string {
    return new Date(jour).getDate().toString();
  }
}
