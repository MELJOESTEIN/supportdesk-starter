import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';

import { Commentaire } from '../ticket.model';
import { FilCommentaires } from './fil-commentaires';

const PUBLIC: Commentaire = {
  id: 1,
  auteurUsername: 'alice',
  auteurNom: 'Alice Durand',
  auteurType: 'CLIENT',
  contenu: 'Bonjour, la facture ne se télécharge pas.',
  creeLe: '2026-08-24T09:12:00Z',
  visibilite: 'PUBLIC',
};

const INTERNE: Commentaire = {
  id: 2,
  auteurUsername: 'bob',
  auteurNom: 'Bob Lefevre',
  auteurType: 'AGENT',
  contenu: 'Ne pas mentionner la migration au client.',
  creeLe: '2026-08-24T09:58:00Z',
  visibilite: 'INTERNE',
};

describe('FilCommentaires', () => {
  function rendre(commentaires: Commentaire[], vueAgent = false): HTMLElement {
    const fixture = TestBed.createComponent(FilCommentaires);
    fixture.componentRef.setInput('commentaires', commentaires);
    fixture.componentRef.setInput('vueAgent', vueAgent);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('affiche le vocabulaire « note interne » pour un commentaire INTERNE', () => {
    const rendu = rendre([INTERNE], true);

    expect(rendu.querySelector('.note')).not.toBeNull();
    expect(rendu.textContent).toContain('NON VISIBLE PAR LE CLIENT');
  });

  it('n\'affiche jamais ce vocabulaire pour un commentaire public', () => {
    const rendu = rendre([PUBLIC]);

    expect(rendu.querySelector('.note')).toBeNull();
    expect(rendu.textContent).not.toContain('NOTE INTERNE');
  });

  /**
   * Le test qui compte : le composant ne filtre rien. S'il reçoit une note interne, il
   * l'affiche — donc c'est en amont qu'il ne faut pas la lui donner. Ce test documente
   * le contrat pour que personne n'ajoute plus tard un filtre à l'affichage en croyant
   * régler un problème de sécurité.
   */
  it('affiche ce qu\'on lui donne, sans filtrer : le filtrage est en amont', () => {
    const rendu = rendre([PUBLIC, INTERNE], true);

    expect(rendu.querySelectorAll('article').length).toBe(2);
    expect(rendu.querySelector('.note')).not.toBeNull();
  });

  it('étiquette la réponse d\'un agent selon le point de vue', () => {
    const agent: Commentaire = { ...PUBLIC, id: 3, auteurType: 'AGENT', auteurNom: 'Bob Lefevre' };

    expect(rendre([agent], true).textContent).toContain('RÉPONSE ENVOYÉE AU CLIENT');
    expect(rendre([agent], false).textContent).toContain('SUPPORT SUPPORTDESK');
  });
});
