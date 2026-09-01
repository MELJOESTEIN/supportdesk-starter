import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { describe, expect, it } from 'vitest';

import { NouveauTicket } from './nouveau-ticket';

/**
 * Le formulaire de création.
 *
 * Ces tests existent à cause de deux défauts trouvés à la main, que rien d'automatique
 * n'avait vus :
 *
 *  - `(ngSubmit)` sans `@angular/forms` : la directive n'existe pas, l'événement n'est
 *    jamais émis, le bouton ne fait rien ;
 *  - la directive `[formField]` reflète `required` dans le DOM, ce qui déclenche la
 *    validation native du navigateur — laquelle **bloque le submit avant Angular**.
 */
describe('NouveauTicket', () => {
  function rendre() {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    const fixture = TestBed.createComponent(NouveauTicket);
    fixture.detectChanges();
    return fixture;
  }

  it('le formulaire porte novalidate : Angular seul valide', () => {
    const form = (rendre().nativeElement as HTMLElement).querySelector('form')!;

    // Sans cet attribut, le navigateur affiche sa propre bulle — non traduisible, non
    // stylable — et l'événement submit n'atteint jamais le composant.
    expect(form.hasAttribute('novalidate')).toBe(true);
  });

  it('le bouton est bien de type submit dans le formulaire', () => {
    const bouton = (rendre().nativeElement as HTMLElement).querySelector(
      '.formulaire__bouton--primaire',
    ) as HTMLButtonElement;

    expect(bouton.type).toBe('submit');
    expect(bouton.closest('form')).not.toBeNull();
  });

  it("n'expose ni statut, ni priorité, ni assignation, ni propriétaire", () => {
    const rendu = (rendre().nativeElement as HTMLElement).innerHTML.toLowerCase();

    // Affectation en masse : un champ affiché ici finirait par être accepté par l'API.
    expect(rendu).not.toContain('statut');
    expect(rendu).not.toContain('priorit');
    expect(rendu).not.toContain('assign');
    expect(rendu).not.toContain('crmclientref');
  });

  it('affiche le message du champ obligatoire après une tentative d\'envoi', async () => {
    const fixture = rendre();
    const rendu = fixture.nativeElement as HTMLElement;

    expect(rendu.querySelector('.champ__aide--erreur')).toBeNull();

    (rendu.querySelector('.formulaire__bouton--primaire') as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();

    // `submit()` bloque bien un formulaire invalide, mais ne marque pas les champs comme
    // touchés : sans le signal de tentative, le refus serait silencieux.
    expect(rendu.querySelector('.champ__aide--erreur')?.textContent?.trim()).toBe(
      'Le sujet est obligatoire',
    );
    expect(rendu.querySelector('#sujet')?.getAttribute('aria-invalid')).toBe('true');
  });
});
