import { Component, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

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

  /**
   * Renseigné à `reserve` quand une garde a renvoyé l'utilisateur depuis la zone agent.
   *
   * <p>Lié par `withComponentInputBinding()` : le paramètre de requête arrive comme une
   * entrée du composant, sans injecter `ActivatedRoute`.
   */
  readonly acces = input<string>('');

  protected readonly renvoyeDeLaZoneAgent = computed(() => this.acces() === 'reserve');

  protected readonly filtre = signal<FiltreTickets>({ page: 0, taille: 6 });

  /**
   * Aucun périmètre n'est envoyé : le serveur le déduit du jeton. Le transmettre depuis
   * ici rouvrirait la porte que le lot 4 vient de fermer.
   */
  protected readonly page = this.service.pageTickets(this.filtre);

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
