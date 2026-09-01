import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { CategorieTicket } from '../ticket.model';

const CATEGORIES: { valeur: CategorieTicket; libelle: string }[] = [
  { valeur: 'FACTURATION', libelle: 'Facturation' },
  { valeur: 'ACCES', libelle: 'Accès et connexion' },
  { valeur: 'ANOMALIE', libelle: 'Anomalie technique' },
  { valeur: 'EVOLUTION', libelle: "Demande d'évolution" },
  { valeur: 'AUTRE', libelle: 'Autre' },
];

const SUJET_MAX = 80;

/**
 * Nouveau ticket (écran 03).
 *
 * Quatre champs, une colonne, un seul bouton primaire. Le formulaire n'expose **ni statut,
 * ni priorité, ni assignation** : ces valeurs sont décidées par le serveur. Un champ affiché
 * ici finirait par être accepté par l'API.
 */
@Component({
  selector: 'sd-nouveau-ticket',
  imports: [RouterLink],
  templateUrl: './nouveau-ticket.html',
  styleUrl: './nouveau-ticket.scss',
})
export class NouveauTicket {
  private readonly router = inject(Router);

  protected readonly categories = CATEGORIES;
  protected readonly sujetMax = SUJET_MAX;

  protected readonly sujet = signal('');
  protected readonly categorie = signal<CategorieTicket>('FACTURATION');
  protected readonly description = signal('');
  protected readonly soumis = signal(false);

  protected sujetInvalide(): boolean {
    return this.soumis() && this.sujet().trim().length === 0;
  }

  protected sujetTropLong(): boolean {
    return this.sujet().length > SUJET_MAX;
  }

  protected envoyer(): void {
    this.soumis.set(true);
    if (this.sujetInvalide() || this.sujetTropLong() || !this.description().trim()) {
      return;
    }
    // Lot 2 : la création n'est pas persistée. Lot 3 : POST /api/tickets.
    void this.router.navigate(['/mes-tickets']);
  }

  protected annuler(): void {
    void this.router.navigate(['/mes-tickets']);
  }
}
