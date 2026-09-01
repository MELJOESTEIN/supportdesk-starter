import { Injectable, Signal, resource } from '@angular/core';

import { PageReponse } from '../core/page.model';
import { TICKETS, detailFixture } from './ticket.fixtures';
import { FiltreTickets, TicketDetail, TicketResume } from './ticket.model';

/** Latence simulée : sans elle, les états de chargement ne seraient jamais visibles. */
const LATENCE_MS = 220;

/**
 * Accès aux tickets.
 *
 * Lot 2 : les données viennent des fixtures, servies par `resource()`.
 * Lot 4 : le corps des loaders devient `httpResource('/api/tickets…')`. La signature
 * publique de ce service ne change pas, donc aucun composant n'est réécrit.
 */
@Injectable({ providedIn: 'root' })
export class TicketService {
  /** Page de tickets. `perimetre` vaut la référence CRM d'un client, ou `null` pour un agent. */
  pageTickets(filtre: Signal<FiltreTickets>, perimetre: Signal<string | null>) {
    return resource<PageReponse<TicketResume>, { filtre: FiltreTickets; perimetre: string | null }>({
      params: () => ({ filtre: filtre(), perimetre: perimetre() }),
      loader: async ({ params, abortSignal }) => {
        await patienter(abortSignal);
        return paginer(filtrer(params.filtre, params.perimetre), params.filtre);
      },
    });
  }

  /**
   * Détail d'un ticket.
   *
   * `pourAgent` décide ce qui est **chargé**, pas ce qui est affiché : un client ne reçoit
   * jamais les commentaires internes. Au lot 4 c'est le backend qui tranche, à partir du
   * jeton — jamais à partir d'un paramètre envoyé par le client.
   */
  detail(id: Signal<number | null>, pourAgent: Signal<boolean>) {
    return resource<TicketDetail | null, { id: number | null; pourAgent: boolean }>({
      params: () => ({ id: id(), pourAgent: pourAgent() }),
      loader: async ({ params, abortSignal }) => {
        if (params.id === null) {
          return null;
        }
        await patienter(abortSignal);
        return detailFixture(params.id, params.pourAgent);
      },
    });
  }

  /** Le ticket existe-t-il mais appartient-il à un autre compte ? (écran 403) */
  appartientAUnAutreCompte(id: number, perimetre: string | null): boolean {
    if (perimetre === null) {
      return false;
    }
    const ticket = TICKETS.find((t) => t.id === id);
    return ticket !== undefined && ticket.crmClientRef !== perimetre;
  }

  referenceProprietaire(id: number): string | null {
    return TICKETS.find((t) => t.id === id)?.clientRaisonSociale ?? null;
  }
}

function patienter(abortSignal: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    const minuteur = setTimeout(resolve, LATENCE_MS);
    abortSignal.addEventListener('abort', () => {
      clearTimeout(minuteur);
      reject(new DOMException('Chargement annulé', 'AbortError'));
    });
  });
}

function filtrer(filtre: FiltreTickets, perimetre: string | null): TicketResume[] {
  let resultat = TICKETS;

  // Le périmètre n'est pas un filtre parmi d'autres : c'est une frontière. Au lot 4 il
  // vient du jeton et la sélection se fait côté serveur.
  if (perimetre !== null) {
    resultat = resultat.filter((t) => t.crmClientRef === perimetre);
  }

  if (filtre.statuts?.length) {
    resultat = resultat.filter((t) => filtre.statuts!.includes(t.statut));
  }
  if (filtre.crmClientRef) {
    resultat = resultat.filter((t) => t.crmClientRef === filtre.crmClientRef);
  }
  if (filtre.assigneA) {
    resultat =
      filtre.assigneA === 'NON_ASSIGNE'
        ? resultat.filter((t) => t.assigneA === null)
        : resultat.filter((t) => t.assigneA?.username === filtre.assigneA);
  }
  if (filtre.recherche) {
    const recherche = filtre.recherche.toLowerCase();
    resultat = resultat.filter(
      (t) =>
        t.sujet.toLowerCase().includes(recherche) ||
        t.reference.toLowerCase().includes(recherche),
    );
  }

  return [...resultat].sort((a, b) => b.derniereActiviteLe.localeCompare(a.derniereActiviteLe));
}

function paginer(
  tickets: TicketResume[],
  filtre: FiltreTickets,
): PageReponse<TicketResume> {
  const taille = filtre.taille ?? 6;
  const page = filtre.page ?? 0;
  const debut = page * taille;

  return {
    contenu: tickets.slice(debut, debut + taille),
    page,
    taille,
    total: tickets.length,
    totalPages: Math.max(1, Math.ceil(tickets.length / taille)),
  };
}
