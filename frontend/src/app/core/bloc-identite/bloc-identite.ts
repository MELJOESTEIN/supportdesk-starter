import { Component, computed, input, output } from '@angular/core';

import { RoleUtilisateur, Utilisateur } from '../../auth/auth.model';
import { BadgeRole } from '../badge-role/badge-role';

/** Ordre de préséance pour l'affichage : un utilisateur ADMIN+AGENT s'affiche ADMIN. */
const PRESEANCE: RoleUtilisateur[] = ['ADMIN', 'AGENT', 'CLIENT'];

/**
 * Bloc d'identité de l'en-tête, présent sur tous les écrans connectés.
 * La déconnexion est une action secondaire, toujours au même endroit, jamais dans un menu.
 */
@Component({
  selector: 'sd-bloc-identite',
  imports: [BadgeRole],
  templateUrl: './bloc-identite.html',
  styleUrl: './bloc-identite.scss',
})
export class BlocIdentite {
  readonly utilisateur = input.required<Utilisateur>();

  /** Raison sociale (client) ou service (agent), affichée sous le nom. */
  readonly rattachement = input<string | null>(null);

  readonly deconnexion = output<void>();

  protected readonly rolePrincipal = computed<RoleUtilisateur>(
    () => PRESEANCE.find((r) => this.utilisateur().roles.includes(r)) ?? 'CLIENT',
  );
}
