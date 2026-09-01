import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

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
  /** Référence du ticket demandé, si la route la connaît. */
  readonly reference = input<string>('');

  /**
   * Le nom du compte propriétaire n'est plus affiché.
   *
   * Le serveur ne le transmet pas : sa réponse 403 ne contient aucune donnée du ticket.
   * La maquette le nommait ; nous préférons ne pas divulguer à quel compte appartient un
   * identifiant. C'est un écart assumé par rapport au design, en faveur de la discrétion.
   *
   * <p>Le libellé se lit « rattaché à un autre compte ». La phrase disait « rattaché au
   * compte un autre compte » : le gabarit avait gardé le mot « compte » de la maquette, où
   * la substitution était un nom de société. Un texte à trous se relit une fois la valeur
   * remplacée, pas seulement une fois écrit.
   */
  protected proprietaire(): string {
    return 'un autre compte';
  }

  protected referenceTentative(): string {
    const maintenant = new Date();
    const jour = maintenant.toISOString().slice(0, 10);
    const heure = maintenant.toTimeString().slice(0, 5).replace(':', '');
    return `ACC-${jour}-${heure}`;
  }
}
