import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';

import { Composeur } from './composeur';

describe('Composeur', () => {
  function rendre(modeAgent: boolean, destinataire = 'Transports Nord') {
    const fixture = TestBed.createComponent(Composeur);
    fixture.componentRef.setInput('modeAgent', modeAgent);
    fixture.componentRef.setInput('destinataire', destinataire);
    fixture.detectChanges();
    return fixture;
  }

  it('côté client, n\'offre aucun moyen d\'écrire une note interne', () => {
    const rendu = rendre(false).nativeElement as HTMLElement;

    expect(rendu.querySelector('.onglets')).toBeNull();
    expect(rendu.textContent).not.toContain('NOTE INTERNE');
    expect(rendu.querySelector('.zone__envoyer')!.textContent).toContain('Envoyer la réponse');
  });

  /** Le destinataire est écrit en clair : on ne peut pas se tromper de mode sans le voir. */
  it('côté agent en mode public, nomme le destinataire au-dessus du champ', () => {
    const rendu = rendre(true, 'Transports Nord').nativeElement as HTMLElement;

    expect(rendu.textContent).toContain('SERA ENVOYÉE À TRANSPORTS NORD');
    expect(rendu.querySelector('.zone__envoyer')!.textContent).toContain('Envoyer au client');
  });

  it('bascule en mode interne : bandeau, hachures et libellé de bouton changent', () => {
    const fixture = rendre(true);
    const rendu = fixture.nativeElement as HTMLElement;

    const ongletInterne = rendu.querySelectorAll('.onglets__onglet')[1] as HTMLButtonElement;
    ongletInterne.click();
    fixture.detectChanges();

    expect(rendu.querySelector('.zone--interne')).not.toBeNull();
    expect(rendu.querySelector('.zone__hachures')).not.toBeNull();
    expect(rendu.textContent).toContain('NON VISIBLE PAR LE CLIENT');
    expect(rendu.querySelector('.zone__envoyer')!.textContent).toContain(
      'Enregistrer la note interne',
    );
    expect(rendu.textContent).not.toContain('SERA ENVOYÉE À');
  });

  it('émet la visibilité choisie, jamais une valeur par défaut silencieuse', () => {
    const fixture = rendre(true);
    const rendu = fixture.nativeElement as HTMLElement;
    const emis: string[] = [];
    fixture.componentInstance.envoyer.subscribe((c) => emis.push(c.visibilite));

    (rendu.querySelectorAll('.onglets__onglet')[1] as HTMLButtonElement).click();
    fixture.detectChanges();

    const saisie = rendu.querySelector('textarea') as HTMLTextAreaElement;
    saisie.value = 'Note pour l\'équipe';
    saisie.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    (rendu.querySelector('.zone__envoyer') as HTMLButtonElement).click();

    expect(emis).toEqual(['INTERNE']);
  });

  it('refuse d\'envoyer un contenu vide', () => {
    const fixture = rendre(false);
    const rendu = fixture.nativeElement as HTMLElement;
    let appels = 0;
    fixture.componentInstance.envoyer.subscribe(() => appels++);

    const bouton = rendu.querySelector('.zone__envoyer') as HTMLButtonElement;
    expect(bouton.disabled).toBe(true);
    bouton.click();

    expect(appels).toBe(0);
  });
});
