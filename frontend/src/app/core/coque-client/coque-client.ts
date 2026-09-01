import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { Session } from '../../auth/session';
import { BlocIdentite } from '../bloc-identite/bloc-identite';

/** Coque de l'espace client : en-tête de 56 px, contenu, aucune barre latérale. */
@Component({
  selector: 'sd-coque-client',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, BlocIdentite],
  templateUrl: './coque-client.html',
  styleUrl: './coque-client.scss',
})
export class CoqueClient {
  protected readonly session = inject(Session);

  protected deconnecter(): void {
    this.session.simulerConnexion(null);
  }
}
