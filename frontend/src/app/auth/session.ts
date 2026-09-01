import { Injectable, computed, signal } from '@angular/core';

import { RoleUtilisateur, Utilisateur } from './auth.model';

/**
 * Identités du realm `supportdesk`. Ce sont les vraies : la maquette affiche
 * « Camille Roussel / Atelier Vernet », mais ces noms-là n'existent nulle part dans
 * Keycloak. La maquette fixe la mise en page, pas les valeurs.
 */
export const UTILISATEUR_CLIENT: Utilisateur = {
  username: 'alice',
  nomComplet: 'Alice Durand',
  email: 'alice@transports-nord.fr',
  roles: ['CLIENT'],
  crmClientRef: 'CLI-0001',
};

export const UTILISATEUR_AGENT: Utilisateur = {
  username: 'bob',
  nomComplet: 'Bob Lefevre',
  email: 'bob@supportdesk.fr',
  roles: ['AGENT'],
  crmClientRef: null,
};

/**
 * Session de l'utilisateur courant.
 *
 * Au lot 2, elle est alimentée par une fixture. Au lot 4, le même signal sera alimenté
 * par les claims du jeton — la signature ne change pas, les composants non plus.
 *
 * Ce que porte cette session sert à décider ce qu'on **affiche**. Ce qu'un utilisateur a
 * le droit d'**obtenir** est décidé par le backend, à partir du même jeton.
 */
@Injectable({ providedIn: 'root' })
export class Session {
  private readonly _utilisateur = signal<Utilisateur | null>(UTILISATEUR_CLIENT);

  readonly utilisateur = this._utilisateur.asReadonly();

  readonly connecte = computed(() => this._utilisateur() !== null);

  readonly estAgent = computed(() => this.aLeRole('AGENT') || this.aLeRole('ADMIN'));

  aLeRole(role: RoleUtilisateur): boolean {
    return this._utilisateur()?.roles.includes(role) ?? false;
  }

  /** Fixture du lot 2 : remplacée au lot 4 par la lecture du jeton. */
  simulerConnexion(utilisateur: Utilisateur | null): void {
    this._utilisateur.set(utilisateur);
  }
}
