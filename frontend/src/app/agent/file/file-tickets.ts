import { Component, computed, inject, signal } from '@angular/core';

import { EtatChargement } from '../../core/etats/etat-chargement';
import { EtatErreur } from '../../core/etats/etat-erreur';
import { EtatVide } from '../../core/etats/etat-vide';
import { Pagination } from '../../ticket/pagination/pagination';
import { TableauTickets } from '../../ticket/tableau-tickets/tableau-tickets';
import { FiltreTickets, StatutTicket } from '../../ticket/ticket.model';
import { TicketService } from '../../ticket/ticket.service';
import { AGENTS } from '../agent.fixtures';

const STATUTS: StatutTicket[] = ['OUVERT', 'EN_COURS', 'RESOLU', 'FERME'];

const GLYPHES: Record<StatutTicket, string> = {
  OUVERT: '●',
  EN_COURS: '◑',
  RESOLU: '✓',
  FERME: '✕',
};

/** File des tickets, côté agent (écran 05). Mêmes lignes, colonnes et filtres en plus. */
@Component({
  selector: 'sd-file-tickets',
  imports: [TableauTickets, Pagination, EtatChargement, EtatVide, EtatErreur],
  templateUrl: './file-tickets.html',
  styleUrl: './file-tickets.scss',
})
export class FileTickets {
  private readonly service = inject(TicketService);

  protected readonly statuts = STATUTS;
  protected readonly agents = AGENTS;
  protected readonly taillesDisponibles = [10, 25, 50];

  protected readonly statutsActifs = signal<StatutTicket[]>(['OUVERT', 'EN_COURS']);
  protected readonly client = signal<string>('');
  protected readonly assigne = signal<string>('');

  protected readonly filtre = computed<FiltreTickets>(() => ({
    statuts: this.statutsActifs().length ? this.statutsActifs() : undefined,
    crmClientRef: this.client() || undefined,
    assigneA: this.assigne() || undefined,
    page: this.page(),
    taille: this.taille(),
  }));

  private readonly page = signal(0);
  private readonly taille = signal(10);

  /** Un agent voit tous les comptes : aucun périmètre client. */
  private readonly perimetre = computed(() => null);

  protected readonly resultats = this.service.pageTickets(this.filtre, this.perimetre);

  protected readonly aDesFiltres = computed(
    () => this.statutsActifs().length > 0 || !!this.client() || !!this.assigne(),
  );

  protected glyphe(statut: StatutTicket): string {
    return GLYPHES[statut];
  }

  protected estActif(statut: StatutTicket): boolean {
    return this.statutsActifs().includes(statut);
  }

  protected basculer(statut: StatutTicket): void {
    this.page.set(0);
    this.statutsActifs.update((actifs) =>
      actifs.includes(statut) ? actifs.filter((s) => s !== statut) : [...actifs, statut],
    );
  }

  protected changerClient(valeur: string): void {
    this.page.set(0);
    this.client.set(valeur);
  }

  protected changerAssigne(valeur: string): void {
    this.page.set(0);
    this.assigne.set(valeur);
  }

  protected allerPage(page: number): void {
    this.page.set(page);
  }

  protected changerTaille(taille: number): void {
    this.page.set(0);
    this.taille.set(taille);
  }

  protected reinitialiser(): void {
    this.statutsActifs.set([]);
    this.client.set('');
    this.assigne.set('');
    this.page.set(0);
  }
}
