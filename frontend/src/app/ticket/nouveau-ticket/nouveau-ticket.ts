import { Component, inject, signal } from '@angular/core';
import { FormField, form, maxLength, required, submit } from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { CategorieTicket, NouveauTicket as ModeleNouveauTicket } from '../ticket.model';
import { TicketService } from '../ticket.service';

const CATEGORIES: { valeur: CategorieTicket; libelle: string }[] = [
  { valeur: 'FACTURATION', libelle: 'Facturation' },
  { valeur: 'ACCES', libelle: 'Accès et connexion' },
  { valeur: 'ANOMALIE', libelle: 'Anomalie technique' },
  { valeur: 'EVOLUTION', libelle: "Demande d'évolution" },
  { valeur: 'AUTRE', libelle: 'Autre' },
];

const SUJET_MAX = 80;

/**
 * Nouveau ticket (écran 03), en **Signal Forms**.
 *
 * <p>Quatre champs, une colonne, un seul bouton primaire. Le formulaire n'expose **ni
 * statut, ni priorité, ni assignation** : ces valeurs sont décidées par le serveur. Un champ
 * affiché ici finirait par être accepté par l'API.
 *
 * <h2>Pourquoi Signal Forms plutôt que des `<input>` pilotés à la main</h2>
 *
 * <p>La première version liait chaque champ à un signal et déclenchait l'envoi par
 * `(ngSubmit)`. <b>Ce n'était pas seulement plus verbeux : ça ne marchait pas.</b>
 * `ngSubmit` est une directive de `@angular/forms` ; sans l'import, Angular la traite comme
 * un événement DOM du même nom, qui n'est jamais émis. Le bouton ne faisait rien, et le
 * compilateur ne pouvait rien dire — la syntaxe est valide.
 *
 * <p>Avec `form()`, la validation, l'état « touché », le message d'erreur et la soumission
 * viennent du framework. Il n'y a plus de câblage à oublier.
 */
@Component({
  selector: 'sd-nouveau-ticket',
  imports: [RouterLink, FormField],
  templateUrl: './nouveau-ticket.html',
  styleUrl: './nouveau-ticket.scss',
})
export class NouveauTicket {
  private readonly service = inject(TicketService);
  private readonly router = inject(Router);

  protected readonly categories = CATEGORIES;
  protected readonly sujetMax = SUJET_MAX;

  /** Le modèle est la source de vérité ; `form()` ne le recopie pas, il l'enveloppe. */
  private readonly modele = signal<ModeleNouveauTicket>({
    sujet: '',
    categorie: 'FACTURATION',
    description: '',
  });

  protected readonly formulaire = form(this.modele, (chemin) => {
    required(chemin.sujet, { message: 'Le sujet est obligatoire' });
    maxLength(chemin.sujet, SUJET_MAX, {
      message: `Le sujet ne doit pas dépasser ${SUJET_MAX} caractères`,
    });
    required(chemin.description, { message: 'La description est obligatoire' });
  });

  protected readonly envoiEnCours = signal(false);
  protected readonly erreurEnvoi = signal<string | null>(null);

  /**
   * Une tentative d'envoi a eu lieu.
   *
   * <p>`submit()` bloque bien un formulaire invalide, mais il ne marque pas les champs comme
   * « touchés » : sans ce signal, le clic ne produisait **aucun message**. Un formulaire qui
   * refuse en silence est aussi frustrant qu'un bouton qui ne répond pas — c'est le même
   * défaut, déplacé d'un cran.
   */
  private readonly tentative = signal(false);

  protected async envoyer(): Promise<void> {
    this.erreurEnvoi.set(null);
    this.envoiEnCours.set(true);

    try {
      await submit(this.formulaire, {
        action: async (champ) => {
          await firstValueFrom(this.service.creer(champ().value()));
          await this.router.navigate(['/mes-tickets']);
          return undefined;
        },
        // Appelée quand la validation échoue : c'est ici qu'on rend les erreurs visibles.
        onInvalid: () => this.tentative.set(true),
      });
    }
    catch {
      this.erreurEnvoi.set(
        "Le ticket n'a pas pu être envoyé. Votre saisie est conservée, réessayez.",
      );
    }
    finally {
      this.envoiEnCours.set(false);
    }
  }

  protected annuler(): void {
    void this.router.navigate(['/mes-tickets']);
  }

  /**
   * Premier message d'erreur d'un champ.
   *
   * <p>Affiché dès que le champ a été touché **ou** qu'un envoi a été tenté : on ne crie pas
   * sur quelqu'un qui n'a pas encore commencé à saisir, mais on ne laisse jamais un refus
   * sans explication.
   */
  protected messageErreur(champ: { errors: () => { message?: string }[]; touched: () => boolean }) {
    if (!champ.touched() && !this.tentative()) {
      return null;
    }
    return champ.errors()[0]?.message ?? null;
  }
}
