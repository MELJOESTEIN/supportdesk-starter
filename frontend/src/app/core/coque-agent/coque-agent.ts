import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { Session, UTILISATEUR_AGENT } from '../../auth/session';
import { BlocIdentite } from '../bloc-identite/bloc-identite';

/** Coque du back-office : barre latérale sombre de 208 px, barre de contexte de 48 px. */
@Component({
  selector: 'sd-coque-agent',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, BlocIdentite],
  templateUrl: './coque-agent.html',
  styleUrl: './coque-agent.scss',
})
export class CoqueAgent {
  protected readonly session = inject(Session);

  constructor() {
    // Fixture du lot 2 : la zone agent affiche l'agent. Au lot 4, c'est le jeton qui
    // décide, et une garde de route empêche un CLIENT d'arriver jusqu'ici.
    if (!this.session.estAgent()) {
      this.session.simulerConnexion(UTILISATEUR_AGENT);
    }
  }

  protected deconnecter(): void {
    this.session.simulerConnexion(null);
  }
}
