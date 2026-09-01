import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { RoleUtilisateur } from './auth.model';
import { Session } from './session';

/**
 * Gardes de route.
 *
 * <p><b>Ce sont des éléments d'expérience utilisateur, pas des protections.</b> Elles évitent
 * d'afficher un écran vide à quelqu'un qui n'a rien à y voir. Elles s'ouvrent en modifiant
 * une variable dans la console du navigateur, et un `curl` ne les rencontre jamais.
 *
 * <h2>Pourquoi ces gardes attendent</h2>
 *
 * <p>`checkAuth()` est un appel réseau. Tant qu'il n'a pas répondu, `connecte()` vaut `false`
 * pour un utilisateur pourtant connecté. Une garde qui décide tout de suite renvoie donc à
 * l'accueil quiconque arrive par un lien direct, un favori ou un simple rechargement — mais
 * laisse passer la même personne qui navigue depuis l'intérieur de l'application, où la
 * découverte a eu le temps de répondre. D'où l'attente de `quandPrete()` : sans elle, la
 * garde est correcte dans les tests et fausse dans un navigateur.
 *
 * <p>Toute règle d'accès existe côté backend — voir {@code TicketService#chargerAutorise} et
 * la chaîne de filtres du resource server. Si une garde était le seul rempart, la donnée
 * serait déjà accessible.
 */
export function gardeConnecte(): CanActivateFn {
  return async () => {
    const session = inject(Session);
    const router = inject(Router);

    await session.quandPrete();

    if (session.connecte()) {
      return true;
    }
    return router.createUrlTree(['/accueil']);
  };
}

/**
 * L'inverse de {@link gardeConnecte} : réserve un écran à qui n'est PAS connecté.
 *
 * <p>Sans elle, `/accueil` affichait son bouton « Se connecter » à quelqu'un de déjà
 * connecté. Deux écrans donnaient alors deux réponses différentes à la même question —
 * `/mes-tickets` montrait une session valide, `/accueil` un bouton de connexion — et c'est
 * ce qui a rendu illisible le diagnostic de la déconnexion unique le 30 août.
 */
export function gardeAnonyme(): CanActivateFn {
  return async () => {
    const session = inject(Session);
    const router = inject(Router);

    await session.quandPrete();

    if (!session.connecte()) {
      return true;
    }
    return router.createUrlTree([session.estAgent() ? '/agent' : '/mes-tickets']);
  };
}

export function gardeRole(...roles: RoleUtilisateur[]): CanActivateFn {
  return async () => {
    const session = inject(Session);
    const router = inject(Router);

    await session.quandPrete();

    if (roles.some((role) => session.aLeRole(role))) {
      return true;
    }
    // Pas d'écran 403 ici : l'utilisateur n'a pas demandé une ressource précise, il s'est
    // trompé de zone. On le ramène chez lui — mais en le lui disant. Le refus était muet,
    // alors que le 403 d'un ticket prend trois paragraphes pour s'expliquer : deux refus,
    // deux standards, et celui qui n'explique rien passe pour une panne.
    if (!session.connecte()) {
      return router.createUrlTree(['/accueil']);
    }
    return router.createUrlTree(['/mes-tickets'], { queryParams: { acces: 'reserve' } });
  };
}
