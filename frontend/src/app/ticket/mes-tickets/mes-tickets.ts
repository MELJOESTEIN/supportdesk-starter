import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Session } from '../../auth/session';
import { EtatChargement } from '../../core/etats/etat-chargement';
import { EtatErreur } from '../../core/etats/etat-erreur';
import { EtatVide } from '../../core/etats/etat-vide';
import { Pagination } from '../pagination/pagination';
import { TableauTickets } from '../tableau-tickets/tableau-tickets';
import { FiltreTickets } from '../ticket.model';
import { TicketService } from '../ticket.service';

/** Liste de mes tickets (écran 01). Tri par dernière activité, pagination serveur. */
@Component({
  selector: 'sd-mes-tickets',
  imports: [RouterLink, TableauTickets, Pagination, EtatChargement, EtatVide, EtatErreur],
  templateUrl: './mes-tickets.html',
  styleUrl: './mes-tickets.scss',
})
export class MesTickets {
  private readonly service = inject(TicketService);
  private readonly session = inject(Session);

  protected readonly filtre = signal<FiltreTickets>({ page: 0, taille: 6 });

  /**
   * Périmètre : la référence CRM du client connecté.
   *
   * Elle vient de la session, pas d'un paramètre d'URL. Au lot 4 elle viendra du jeton et
   * la sélection se fera côté serveur — ici, c'est déjà la même règle, appliquée un cran
   * plus bas.
   */
  private readonly perimetre = computed(() => this.session.utilisateur()?.crmClientRef ?? null);

  protected readonly page = this.service.pageTickets(this.filtre, this.perimetre);

  protected readonly recherche = signal('');

  protected readonly aDesFiltres = computed(() => this.recherche().trim().length > 0);

  protected chercher(valeur: string): void {
    this.recherche.set(valeur);
    this.filtre.update((f) => ({ ...f, recherche: valeur || undefined, page: 0 }));
  }

  protected allerPage(page: number): void {
    this.filtre.update((f) => ({ ...f, page }));
  }

  protected reinitialiser(): void {
    this.recherche.set('');
    this.filtre.set({ page: 0, taille: 6 });
  }
}
