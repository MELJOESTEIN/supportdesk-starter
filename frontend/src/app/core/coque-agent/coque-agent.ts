import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { Session } from '../../auth/session';
import { FiltreTickets } from '../../ticket/ticket.model';
import { TicketService } from '../../ticket/ticket.service';
import { BlocIdentite } from '../bloc-identite/bloc-identite';

/** Coque du back-office : barre latérale sombre de 208 px, barre de contexte de 48 px. */
@Component({
  selector: 'sd-coque-agent',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, BlocIdentite],
  templateUrl: './coque-agent.html',
  styleUrl: './coque-agent.scss',
})
export class CoqueAgent {
  protected readonly session = inject(Session);
  private readonly tickets = inject(TicketService);

  /**
   * Compteur de la file, réellement compté.
   *
   * <p>Il valait `247`, écrit en dur dans le gabarit depuis la maquette, à côté d'une file
   * qui en contenait 29. C'est le défaut le plus banal d'une interface construite à partir
   * d'un design : une valeur d'illustration qu'on oublie de brancher, et qui reste crédible
   * assez longtemps pour qu'on la cite en réunion.
   *
   * <p>Il compte ce que l'agent verra en cliquant — la vue par défaut de la file, ouverts et
   * en cours — et non l'ensemble des tickets : un badge qui annonce autre chose que la page
   * qu'il ouvre est un deuxième mensonge, plus discret.
   *
   * <p>`taille: 1` : seul le total nous intéresse, inutile de rapatrier une page entière.
   */
  private readonly filtreFile = signal<FiltreTickets>({
    statuts: ['OUVERT', 'EN_COURS'],
    taille: 1,
  });

  private readonly pageFile = this.tickets.pageTickets(this.filtreFile);

  protected readonly nombreDansLaFile = computed(() => this.pageFile.value()?.total ?? null);

  protected deconnecter(): void {
    this.session.deconnecter();
  }
}
