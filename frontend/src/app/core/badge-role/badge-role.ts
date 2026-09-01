import { Component, computed, input } from '@angular/core';

import { RoleUtilisateur } from '../../auth/auth.model';

const TRAITEMENTS: Record<RoleUtilisateur, { libelle: string; glyphe: string }> = {
  CLIENT: { libelle: 'CLIENT', glyphe: '○' },
  AGENT: { libelle: 'AGENT', glyphe: '◆' },
  ADMIN: { libelle: 'ADMINISTRATEUR', glyphe: '▲' },
};

/**
 * Seul élément de l'interface à porter un badge plein : le rôle doit se lire sans être
 * cherché. Chaque rôle a son glyphe en plus de sa couleur.
 */
@Component({
  selector: 'sd-badge-role',
  template: `
    <span class="role" [class]="'role--' + role().toLowerCase()">
      <span aria-hidden="true">{{ traitement().glyphe }}</span>{{ traitement().libelle }}
    </span>
  `,
  styleUrl: './badge-role.scss',
})
export class BadgeRole {
  readonly role = input.required<RoleUtilisateur>();

  protected readonly traitement = computed(() => TRAITEMENTS[this.role()]);
}
