import { Component, computed, inject, input, numberAttribute } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AccesRefuse } from '../../auth/acces-refuse/acces-refuse';
import { Session } from '../../auth/session';
import { EtatChargement } from '../../core/etats/etat-chargement';
import { EtatErreur } from '../../core/etats/etat-erreur';
import { DateCourtePipe } from '../../core/format/date-courte-pipe';
import { DateLonguePipe } from '../../core/format/date-longue-pipe';
import { DateRelativePipe } from '../../core/format/date-relative-pipe';
import { BadgeStatut } from '../badge-statut/badge-statut';
import { Composeur } from '../composeur/composeur';
import { CategoriePipe } from '../categorie-pipe';
import { FilCommentaires } from '../fil-commentaires/fil-commentaires';
import { NouveauCommentaire } from '../ticket.model';
import { TicketService } from '../ticket.service';

/**
 * Détail d'un ticket, côté client (écran 02).
 *
 * Le fil ne reçoit que des commentaires publics : le service ne charge pas les autres.
 * Aucun `@if` ne masque une note interne ici — s'il y en avait un, la donnée serait déjà
 * arrivée dans le navigateur.
 */
@Component({
  selector: 'sd-detail-ticket',
  imports: [
    RouterLink,
    BadgeStatut,
    FilCommentaires,
    Composeur,
    AccesRefuse,
    EtatChargement,
    EtatErreur,
    DateCourtePipe,
    DateLonguePipe,
    DateRelativePipe,
    CategoriePipe,
  ],
  templateUrl: './detail-ticket.html',
  styleUrl: './detail-ticket.scss',
})
export class DetailTicket {
  readonly id = input.required({ transform: numberAttribute });

  private readonly service = inject(TicketService);
  private readonly session = inject(Session);

  private readonly perimetre = computed(() => this.session.utilisateur()?.crmClientRef ?? null);

  /**
   * L'écran 403 s'affiche sans avoir chargé le ticket.
   *
   * Au lot 4, cette décision appartiendra au backend (403 sur la réponse HTTP) ; le front
   * ne fera que rendre l'écran correspondant. Ici, on ne demande simplement pas la donnée.
   */
  protected readonly refuse = computed(() =>
    this.service.appartientAUnAutreCompte(this.id(), this.perimetre()),
  );

  private readonly idCharge = computed(() => (this.refuse() ? null : this.id()));
  private readonly pourAgent = computed(() => false);

  protected readonly ticket = this.service.detail(this.idCharge, this.pourAgent);

  protected readonly usernameCourant = computed(() => this.session.utilisateur()?.username ?? null);

  protected repondre(commentaire: NouveauCommentaire): void {
    // Lot 2 : pas de persistance. Lot 3 : POST /api/tickets/{id}/commentaires.
    void commentaire;
    this.ticket.reload();
  }
}
