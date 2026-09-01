import { Component, input, output, signal } from '@angular/core';

import { NouveauCommentaire, VisibiliteCommentaire } from '../ticket.model';

/**
 * Zone de réponse.
 *
 * Côté client : un seul mode, une seule issue possible — la réponse est publique.
 * Côté agent : deux modes, et **le destinataire est écrit en clair au-dessus du champ**.
 * La bascule change la couleur, la bordure, la police de saisie et le libellé du bouton :
 * on ne peut pas envoyer une note interne en croyant répondre au client.
 */
@Component({
  selector: 'sd-composeur',
  templateUrl: './composeur.html',
  styleUrl: './composeur.scss',
})
export class Composeur {
  /** Deux modes côté agent, un seul côté client. */
  readonly modeAgent = input(false);

  /** Nom du client, affiché dans le bandeau du mode public. */
  readonly destinataire = input<string>('');

  readonly envoyer = output<NouveauCommentaire>();

  protected readonly visibilite = signal<VisibiliteCommentaire>('PUBLIC');
  protected readonly contenu = signal('');
  protected readonly basculerStatut = signal(false);

  protected choisir(visibilite: VisibiliteCommentaire): void {
    this.visibilite.set(visibilite);
  }

  protected saisir(valeur: string): void {
    this.contenu.set(valeur);
  }

  protected soumettre(): void {
    const contenu = this.contenu().trim();
    if (!contenu) {
      return;
    }
    this.envoyer.emit({ contenu, visibilite: this.visibilite() });
    this.contenu.set('');
  }
}
