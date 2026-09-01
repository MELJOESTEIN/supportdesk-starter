import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';

import { TicketService } from './ticket.service';

/**
 * Le périmètre n'est pas un filtre parmi d'autres : c'est une frontière.
 * Ces tests sont le pendant côté front de la vérification de propriétaire du lot 4.
 */
describe('TicketService — périmètre client', () => {
  function service(): TicketService {
    return TestBed.inject(TicketService);
  }

  it('reconnaît qu\'un ticket appartient à un autre compte', () => {
    const s = service();
    // Le ticket 9 appartient à CLI-0002 dans les fixtures.
    expect(s.appartientAUnAutreCompte(9, 'CLI-0001')).toBe(true);
    expect(s.appartientAUnAutreCompte(9, 'CLI-0002')).toBe(false);
  });

  it('ne refuse rien à un agent (périmètre nul)', () => {
    expect(service().appartientAUnAutreCompte(9, null)).toBe(false);
  });

  it('ne prétend pas qu\'un ticket inexistant appartient à autrui', () => {
    // Un identifiant inconnu doit donner un 404, pas un 403 : ne pas révéler l'inexistence
    // sous couvert d'autorisation.
    expect(service().appartientAUnAutreCompte(9999, 'CLI-0001')).toBe(false);
  });
});
