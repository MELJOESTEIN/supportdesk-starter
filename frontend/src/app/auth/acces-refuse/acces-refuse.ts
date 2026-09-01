import { Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { TicketService } from '../../ticket/ticket.service';

/**
 * « Ce ticket appartient à un autre compte » — 403 (écran 07).
 *
 * C'est la réponse visible de la faille BOLA corrigée au lot 4. Trois partis pris :
 * la coque de l'application connectée est conservée (on est identifié, mais pas
 * autorisé) ; le ton n'accuse pas l'utilisateur ; et la page affiche une référence de
 * tentative en précisant qu'**aucune donnée du ticket n'a été affichée** — ce qui doit
 * rester vrai côté serveur, pas seulement à l'écran.
 */
@Component({
  selector: 'sd-acces-refuse',
  imports: [RouterLink],
  templateUrl: './acces-refuse.html',
  styleUrl: './acces-refuse.scss',
})
export class AccesRefuse {
  /** Référence du ticket demandé, transmise par la route. */
  readonly reference = input<string>('');

  /** Identifiant du ticket demandé, pour retrouver le compte propriétaire. */
  readonly id = input<number | null>(null);

  private readonly tickets = inject(TicketService);

  protected proprietaire(): string {
    const id = this.id();
    return (id !== null ? this.tickets.referenceProprietaire(id) : null) ?? 'un autre compte';
  }

  protected referenceTentative(): string {
    const maintenant = new Date();
    const jour = maintenant.toISOString().slice(0, 10);
    const heure = maintenant.toTimeString().slice(0, 5).replace(':', '');
    return `ACC-${jour}-${heure}`;
  }
}
