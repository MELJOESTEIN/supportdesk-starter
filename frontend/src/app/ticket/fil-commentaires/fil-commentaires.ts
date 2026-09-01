import { Component, input } from '@angular/core';

import { DateRelativePipe } from '../../core/format/date-relative-pipe';
import { DateLonguePipe } from '../../core/format/date-longue-pipe';
import { Commentaire } from '../ticket.model';

/**
 * Fil chronologique d'un ticket.
 *
 * **Ce composant ne filtre rien.** Il affiche exactement ce qu'on lui donne. Une note
 * interne qui arriverait ici pour un client serait déjà une fuite : elle aurait traversé
 * le réseau. Le filtrage se fait à la source — requête SQL au lot 4, fixture au lot 2.
 *
 * Le vocabulaire « note interne » (aubergine, hachures, cadenas, bandeau « NON VISIBLE PAR
 * LE CLIENT », décalage horizontal) n'existe nulle part ailleurs dans l'application : il ne
 * signifie qu'une seule chose.
 */
@Component({
  selector: 'sd-fil-commentaires',
  imports: [DateRelativePipe, DateLonguePipe],
  templateUrl: './fil-commentaires.html',
  styleUrl: './fil-commentaires.scss',
})
export class FilCommentaires {
  readonly commentaires = input.required<Commentaire[]>();

  /** Côté agent, l'auteur d'une réponse publique est étiqueté « RÉPONSE ENVOYÉE AU CLIENT ». */
  readonly vueAgent = input(false);

  /** Nom d'usage du client connecté, pour afficher « vous » sur ses propres messages. */
  readonly usernameCourant = input<string | null>(null);

  protected initiales(nom: string): string {
    return nom
      .split(' ')
      .map((mot) => mot.charAt(0))
      .join('')
      .slice(0, 2)
      .toUpperCase();
  }
}
