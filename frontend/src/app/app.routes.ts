import { Routes } from '@angular/router';

import { gardeAnonyme, gardeConnecte, gardeRole } from './auth/garde-role';

/**
 * Routage par `loadComponent` : chaque écran est chargé à la demande.
 *
 * **Les gardes ci-dessous sont de l'expérience utilisateur, pas des protections.** Elles
 * évitent d'afficher un écran vide à quelqu'un qui n'a rien à y voir. Un `curl` ne les
 * rencontre jamais, et une variable modifiée dans la console du navigateur les ouvre.
 *
 * La protection réelle est côté backend : chaîne de filtres du resource server pour les
 * fonctions, `TicketService#chargerAutorise` pour les données.
 */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'accueil' },

  {
    path: 'accueil',
    title: 'SupportDesk',
    // Quelqu'un de connecté n'a rien à faire sur l'écran de connexion : il y verrait un
    // bouton « Se connecter » alors que sa session est valide.
    canActivate: [gardeAnonyme()],
    loadComponent: () => import('./auth/accueil/accueil').then((m) => m.Accueil),
  },
  { path: 'connexion', pathMatch: 'full', redirectTo: 'connexion/retour' },
  {
    path: 'connexion/retour',
    title: 'Connexion — SupportDesk',
    loadComponent: () =>
      import('./auth/retour-connexion/retour-connexion').then((m) => m.RetourConnexion),
  },
  {
    path: 'deconnexion',
    title: 'Session terminée — SupportDesk',
    loadComponent: () =>
      import('./auth/session-terminee/session-terminee').then((m) => m.SessionTerminee),
  },

  // --- Espace client ---
  {
    path: '',
    // Sans cette garde, un visiteur déconnecté atteint l'écran, l'écran appelle l'API, l'API
    // répond 401, et l'intercepteur le sort de l'application. La garde évite ce détour ; elle
    // ne protège rien — c'est le backend qui refuse la donnée.
    canActivate: [gardeConnecte()],
    loadComponent: () => import('./core/coque-client/coque-client').then((m) => m.CoqueClient),
    children: [
      {
        path: 'mes-tickets',
        title: 'Mes tickets — SupportDesk',
        loadComponent: () => import('./ticket/mes-tickets/mes-tickets').then((m) => m.MesTickets),
      },
      {
        path: 'mes-tickets/nouveau',
        title: 'Nouveau ticket — SupportDesk',
        loadComponent: () =>
          import('./ticket/nouveau-ticket/nouveau-ticket').then((m) => m.NouveauTicket),
      },
      {
        path: 'tickets/:id',
        title: 'Ticket — SupportDesk',
        loadComponent: () =>
          import('./ticket/detail-ticket/detail-ticket').then((m) => m.DetailTicket),
      },
      {
        path: 'acces-refuse',
        title: 'Accès non autorisé — SupportDesk',
        loadComponent: () => import('./auth/acces-refuse/acces-refuse').then((m) => m.AccesRefuse),
      },
    ],
  },

  // --- Back-office agent ---
  {
    path: 'agent',
    // Masque la zone à un client. Le backend refuse de toute façon /api/tableau-de-bord
    // et /api/agents à qui n'a pas le rôle : cette garde ne fait qu'éviter un écran vide.
    canActivate: [gardeRole('AGENT', 'ADMIN')],
    loadComponent: () => import('./core/coque-agent/coque-agent').then((m) => m.CoqueAgent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'Tableau de bord — SupportDesk',
        loadComponent: () =>
          import('./tableau-de-bord/page/tableau-de-bord-page').then((m) => m.TableauDeBordPage),
      },
      {
        path: 'file',
        title: 'File des tickets — SupportDesk',
        loadComponent: () => import('./agent/file/file-tickets').then((m) => m.FileTickets),
      },
      {
        path: 'tickets/:id',
        title: 'Ticket — SupportDesk',
        loadComponent: () =>
          import('./agent/detail-agent/detail-agent').then((m) => m.DetailAgent),
      },
    ],
  },

  { path: '**', redirectTo: 'accueil' },
];
