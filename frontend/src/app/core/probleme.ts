import { HttpErrorResponse } from '@angular/common/http';

import { ProblemDetail } from './page.model';

/** Préfixe des types d'erreur du backend. Voir GestionnaireErreurs, côté Java. */
const BASE = 'https://supportdesk.local/erreurs/';

export const TYPE_AUTRE_COMPTE = `${BASE}ticket-autre-compte`;
export const TYPE_COMPTE_NON_RATTACHE = `${BASE}compte-non-rattache`;
export const TYPE_INTROUVABLE = `${BASE}ticket-introuvable`;

/**
 * Lit le `type` d'un ProblemDetail.
 *
 * <p>C'est sur cette URI stable que le client branche sa logique — jamais sur `detail`,
 * qui est du texte destiné à un humain et peut être traduit ou reformulé sans préavis.
 */
export function typeDeProbleme(erreur: unknown): string | null {
  if (!(erreur instanceof HttpErrorResponse)) {
    return null;
  }
  const corps = erreur.error as ProblemDetail | null;
  return corps?.type ?? null;
}

export function estAutreCompte(erreur: unknown): boolean {
  return typeDeProbleme(erreur) === TYPE_AUTRE_COMPTE;
}

export function estIntrouvable(erreur: unknown): boolean {
  return typeDeProbleme(erreur) === TYPE_INTROUVABLE;
}
