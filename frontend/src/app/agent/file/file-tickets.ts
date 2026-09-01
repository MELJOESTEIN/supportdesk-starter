import { Component, computed, inject, signal } from '@angular/core';

import { EtatChargement } from '../../core/etats/etat-chargement';
import { EtatErreur } from '../../core/etats/etat-erreur';
import { EtatVide } from '../../core/etats/etat-vide';
import { Pagination } from '../../ticket/pagination/pagination';
import { TableauTickets } from '../../ticket/tableau-tickets/tableau-tickets';
import { FiltreTickets, StatutTicket } from '../../ticket/ticket.model';
import { TicketService } from '../../ticket/ticket.service';
import { AgentService } from '../agent.service';

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
  private readonly agentService = inject(AgentService);

  protected readonly statuts = STATUTS;
  protected readonly agents = this.agentService.lister();
  protected readonly taillesDisponibles = [10, 25, 50];

  /**
   * Vue par défaut : ce qui demande un traitement. Un agent n'ouvre pas son back-office
   * pour relire des tickets fermés.
   */
  private static readonly STATUTS_PAR_DEFAUT: StatutTicket[] = ['OUVERT', 'EN_COURS'];

  protected readonly statutsActifs = signal<StatutTicket[]>([...FileTickets.STATUTS_PAR_DEFAUT]);
  protected readonly client = signal<string>('');

  /**
   * Les sociétés proposées au filtre, telles qu'elles existent dans la file.
   *
   * <p>Cette liste était écrite en dur dans le template — cinq entrées recopiées de la
   * maquette. La file en compte sept : les tickets des deux autres s'affichaient sans
   * pouvoir être isolés. Un filtre qui ne couvre pas ce qu'il affiche est pire qu'un filtre
   * absent, parce qu'on lui fait confiance.
   */
  protected readonly clients = this.service.optionsClients();
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

  /** Un agent voit tous les comptes : le serveur ne borne rien pour lui. */
  protected readonly resultats = this.service.pageTickets(this.filtre);

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

  /**
   * Revient à la vue par défaut.
   *
   * <p>« Réinitialiser » désactivait toutes les puces et affichait les 29 tickets — un état
   * où l'on n'était jamais passé. Le bouton vidait les filtres au lieu de restaurer la vue
   * de départ, et l'agent qui cherchait son écran de travail ne le retrouvait pas.
   */
  protected reinitialiser(): void {
    this.statutsActifs.set([...FileTickets.STATUTS_PAR_DEFAUT]);
    this.client.set('');
    this.assigne.set('');
    this.page.set(0);
  }

  /**
   * Retire toute restriction, y compris les statuts.
   *
   * <p>Distinct de {@link reinitialiser} : « Voir toute la file » et « Retirer les filtres »
   * portaient deux libellés pour un seul comportement. Deux intentions différentes méritent
   * deux gestes — sinon l'un des deux boutons ment.
   */
  protected voirToutLaFile(): void {
    this.statutsActifs.set([]);
    this.client.set('');
    this.assigne.set('');
    this.page.set(0);
  }
}
