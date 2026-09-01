/**
 * Page renvoyée par l'API.
 *
 * Ce n'est volontairement pas la forme de `PageImpl` de Spring Data : sa sérialisation
 * n'est pas un contrat stable et Spring lui-même déconseille de l'exposer. Le backend
 * construit ce type explicitement.
 */
export interface PageReponse<T> {
  contenu: T[];
  page: number;
  taille: number;
  total: number;
  totalPages: number;
}

/** Réponse d'erreur de l'API — RFC 9457, `application/problem+json`. */
export interface ProblemDetail {
  /** URI stable : c'est sur elle que le client branche sa logique, jamais sur `detail`. */
  type: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  /** Propriétés d'extension ajoutées par le backend (ex. `champs` pour une validation). */
  [extension: string]: unknown;
}
