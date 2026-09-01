import { Component, computed, inject, input, numberAttribute } from '@angular/core';
import { RouterLink } from '@angular/router';

import { EtatChargement } from '../../core/etats/etat-chargement';
import { EtatErreur } from '../../core/etats/etat-erreur';
import { DateRelativePipe } from '../../core/format/date-relative-pipe';
import { BadgeStatut } from '../../ticket/badge-statut/badge-statut';
import { Composeur } from '../../ticket/composeur/composeur';
import { FilCommentaires } from '../../ticket/fil-commentaires/fil-commentaires';
import { NouveauCommentaire, PrioriteTicket, StatutTicket } from '../../ticket/ticket.model';
import { TicketService } from '../../ticket/ticket.service';
import { AGENTS } from '../agent.fixtures';

const STATUTS: StatutTicket[] = ['OUVERT', 'EN_COURS', 'RESOLU', 'FERME'];
const PRIORITES: PrioriteTicket[] = ['BASSE', 'NORMALE', 'HAUTE'];

/**
 * Détail d'un ticket, côté agent (écran 06).
 *
 * Le fil mêle réponses publiques et notes internes. Les cinq signaux qui les distinguent
 * sont portés par `sd-fil-commentaires` ; ici on se contente de lui passer la liste
 * complète — un agent a le droit de tout voir, contrairement au client.
 */
@Component({
  selector: 'sd-detail-agent',
  imports: [
    RouterLink,
    BadgeStatut,
    FilCommentaires,
    Composeur,
    EtatChargement,
    EtatErreur,
    DateRelativePipe,
  ],
  templateUrl: './detail-agent.html',
  styleUrl: './detail-agent.scss',
})
export class DetailAgent {
  readonly id = input.required({ transform: numberAttribute });

  private readonly service = inject(TicketService);

  protected readonly statuts = STATUTS;
  protected readonly priorites = PRIORITES;
  protected readonly agents = AGENTS;

  private readonly idSignal = computed(() => this.id());
  private readonly pourAgent = computed(() => true);

  protected readonly ticket = this.service.detail(this.idSignal, this.pourAgent);

  protected readonly notesInternes = computed(
    () => this.ticket.value()?.commentaires.filter((c) => c.visibilite === 'INTERNE').length ?? 0,
  );

  protected readonly messagesPublics = computed(
    () => this.ticket.value()?.commentaires.filter((c) => c.visibilite === 'PUBLIC').length ?? 0,
  );

  protected enregistrer(commentaire: NouveauCommentaire): void {
    // Lot 2 : pas de persistance. Lot 3 : POST /api/tickets/{id}/commentaires.
    void commentaire;
    this.ticket.reload();
  }
}
