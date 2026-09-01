import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Accueil non connecté (écran 08).
 *
 * Aucun menu, aucune donnée, et surtout **aucun champ de saisie** : l'identification a lieu
 * sur Keycloak. L'application ne voit jamais d'identifiant ni de mot de passe — c'est tout
 * l'intérêt d'un fournisseur d'identité, et ça se voit à l'écran.
 */
@Component({
  selector: 'sd-accueil',
  imports: [RouterLink],
  template: `
    <div class="accueil">
      <div class="accueil__carte">
        <div class="accueil__marque">SUPPORTDESK</div>
        <p class="accueil__texte">
          L'espace de suivi des demandes de support. Vos tickets, leurs réponses et leur
          avancement, au même endroit.
        </p>
        <a routerLink="/connexion" class="accueil__bouton">Se connecter</a>
        <p class="accueil__mention">
          Vous serez redirigé vers le portail d'identité de votre organisation.
        </p>
      </div>
    </div>
  `,
  styleUrl: './accueil.scss',
})
export class Accueil {}
