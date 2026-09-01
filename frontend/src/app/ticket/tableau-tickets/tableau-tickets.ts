import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

import { DateCourtePipe } from '../../core/format/date-courte-pipe';
import { DateLonguePipe } from '../../core/format/date-longue-pipe';
import { DateRelativePipe } from '../../core/format/date-relative-pipe';
import { NomAbregePipe } from '../../core/format/nom-abrege-pipe';
import { BadgeStatut } from '../badge-statut/badge-statut';
import { TicketResume } from '../ticket.model';

export type VarianteTableau = 'client' | 'agent';

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
}
