export type RoleUtilisateur = 'CLIENT' | 'AGENT' | 'ADMIN';

/**
 * Utilisateur connecté, reconstitué **à partir du jeton** — jamais d'un appel qui
 * demanderait « qui suis-je ? » en passant un identifiant.
 *
 * Rappel du projet : ce que porte cet objet sert à décider ce qu'on **affiche**.
 * Ce qu'un utilisateur a le droit d'**obtenir** est décidé côté backend, à partir du
 * même jeton. Un guard de route est de l'UX.
 */
export interface Utilisateur {
  username: string;
  nomComplet: string;
  email: string | null;
  roles: RoleUtilisateur[];
  /** Présente pour un CLIENT, absente pour un AGENT ou un ADMIN. */
  crmClientRef: string | null;
}
