import { Routes } from '@angular/router';

/**
 * Routage par `loadComponent` : chaque écran est chargé à la demande.
 *
 * **Aucune garde ici.** Elles arrivent au lot 4, et resteront de l'UX : ce sont les
 * vérifications côté backend qui protègent les données. Une route agent atteinte par un
 * client doit être refusée par l'API, pas seulement masquée par le routeur.
 */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'accueil' },

  {
    path: 'accueil',
    title: 'SupportDesk',
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
