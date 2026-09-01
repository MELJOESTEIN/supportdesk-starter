import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';

import { StatutTicket } from '../ticket.model';
import { BadgeStatut } from './badge-statut';

/**
 * Le badge porte quatre signaux : couleur, forme, glyphe, libellé.
 * Ces tests vérifient les trois qui ne sont pas la couleur — ce sont eux qui font que
 * l'information survit en niveaux de gris.
 */
describe('BadgeStatut', () => {
  function rendre(statut: StatutTicket): HTMLElement {
    const fixture = TestBed.createComponent(BadgeStatut);
    fixture.componentRef.setInput('statut', statut);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('donne un glyphe distinct à chacun des quatre statuts', () => {
    const glyphes = (['OUVERT', 'EN_COURS', 'RESOLU', 'FERME'] as StatutTicket[]).map(
      (statut) => rendre(statut).querySelector('.badge__glyphe')!.textContent!.trim(),
    );

    expect(glyphes).toEqual(['●', '◑', '✓', '✕']);
    expect(new Set(glyphes).size).toBe(4);
  });

  it('donne une classe de forme distincte à chacun des quatre statuts', () => {
    const classes = (['OUVERT', 'EN_COURS', 'RESOLU', 'FERME'] as StatutTicket[]).map(
      (statut) => rendre(statut).querySelector('.badge')!.className,
    );

    expect(new Set(classes).size).toBe(4);
  });

  it('affiche le libellé en toutes lettres, pas seulement une pastille', () => {
    expect(rendre('EN_COURS').textContent).toContain('EN_COURS');
  });

  it('barre le libellé de FERME', () => {
    const ferme = rendre('FERME').querySelector('.badge__libelle')!;
    const ouvert = rendre('OUVERT').querySelector('.badge__libelle')!;

    expect(ferme.className).toContain('badge__libelle--barre');
    expect(ouvert.className).not.toContain('badge__libelle--barre');
  });

  it('donne une description textuelle aux lecteurs d\'écran', () => {
    expect(rendre('RESOLU').querySelector('.sd-hors-ecran')!.textContent).toContain('résolu');
  });
});
