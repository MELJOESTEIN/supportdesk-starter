import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

import { DateCourtePipe } from '../../core/format/date-courte-pipe';
import { DateLonguePipe } from '../../core/format/date-longue-pipe';
import { DateRelativePipe } from '../../core/format/date-relative-pipe';
import { NomAbregePipe } from '../../core/format/nom-abrege-pipe';
import { BadgeStatut } from '../badge-statut/badge-statut';
import { PrioriteTicket, TicketResume } from '../ticket.model';

export type VarianteTableau = 'client' | 'agent';

/**
 * Glyphes de priorité.
 *
 * <p>La colonne PRIORITÉ ne se distinguait que par une couleur et une graisse. Les badges de
 * statut portent chacun trois marqueurs non chromatiques ; la priorité n'en avait aucun. En
 * noir et blanc, ou pour un daltonien protanope, l'urgence disparaissait — alors que c'est
 * l'information qu'un agent balaie en premier.
 *
 * <p>Trois formes, pas trois couleurs : la flèche pleine monte, le tiret est neutre, la
 * flèche creuse descend. Lisible à la photocopie.
 */
const GLYPHES_PRIORITE: Record<PrioriteTicket, string> = {
  HAUTE: '▲',
  NORMALE: '–',
  BASSE: '▽',
};

/**
 * Tableau dense de tickets, tri par dernière activité.
 *
 * Deux variantes de colonnes — portail client (écran 01) et file agent (écran 05) — parce
 * que ce sont les mêmes lignes, la même densité et le même pied de page. Ce qui change,
 * ce sont les colonnes que chaque profil a le droit de voir.
 */
@Component({
  selector: 'sd-tableau-tickets',
  imports: [
    RouterLink,
    BadgeStatut,
    DateCourtePipe,
    DateRelativePipe,
    DateLonguePipe,
    NomAbregePipe,
  ],
  templateUrl: './tableau-tickets.html',
  styleUrl: './tableau-tickets.scss',
})
export class TableauTickets {
  readonly tickets = input.required<TicketResume[]>();
  readonly variante = input<VarianteTableau>('client');
  /** Base des liens : `/tickets` côté client, `/agent/tickets` côté back-office. */
  readonly baseLien = input('/tickets');

  readonly selection = output<TicketResume>();

  protected glyphePriorite(priorite: PrioriteTicket): string {
    return GLYPHES_PRIORITE[priorite];
  }
}
