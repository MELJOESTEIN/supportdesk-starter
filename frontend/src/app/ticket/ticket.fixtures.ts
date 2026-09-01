import { AGENT_PAR_USERNAME } from '../agent/agent.fixtures';
import {
  CategorieTicket,
  Commentaire,
  EvenementTicket,
  PrioriteTicket,
  StatutTicket,
  TicketDetail,
  TicketResume,
} from './ticket.model';

/**
 * Jeu de données en mémoire du lot 2.
 *
 * Il disparaît au lot 4, quand le front est branché sur le vrai backend. Les valeurs
 * reprennent celles de la maquette (références TCK-48xx, sujets, raisons sociales) pour
 * que la comparaison écran par écran soit possible.
 */
interface Graine {
  reference: string;
  sujet: string;
  statut: StatutTicket;
  priorite: PrioriteTicket;
  categorie: CategorieTicket;
  crmClientRef: string;
  clientRaisonSociale: string;
  assigneA: string | null;
  creeLe: string;
  derniereActiviteLe: string;
  derniereActivitePar: string | null;
  nombreMessages: number;
  slaDepasse: boolean;
}

const GRAINES: Graine[] = [
  // --- Tickets du client CLI-0001 (alice), visibles sur l'écran 01 ---
  { reference: 'TCK-4821', sujet: 'Facture de mars non téléchargeable', statut: 'OUVERT', priorite: 'NORMALE', categorie: 'FACTURATION', crmClientRef: 'CLI-0001', clientRaisonSociale: 'Transports Nord', assigneA: 'bob', creeLe: '2026-08-24T09:12:00Z', derniereActiviteLe: '2026-08-29T08:26:00Z', derniereActivitePar: 'vous', nombreMessages: 3, slaDepasse: false },
  { reference: 'TCK-4790', sujet: 'Accès refusé au tableau de bord depuis lundi', statut: 'EN_COURS', priorite: 'HAUTE', categorie: 'ACCES', crmClientRef: 'CLI-0001', clientRaisonSociale: 'Transports Nord', assigneA: 'bob', creeLe: '2026-08-19T14:03:00Z', derniereActiviteLe: '2026-08-29T06:40:00Z', derniereActivitePar: 'Bob Lefevre', nombreMessages: 7, slaDepasse: false },
  { reference: 'TCK-4712', sujet: 'Ajouter un second utilisateur au contrat', statut: 'EN_COURS', priorite: 'BASSE', categorie: 'EVOLUTION', crmClientRef: 'CLI-0001', clientRaisonSociale: 'Transports Nord', assigneA: 'carol', creeLe: '2026-08-11T10:20:00Z', derniereActiviteLe: '2026-08-28T17:42:00Z', derniereActivitePar: 'Carol Nguyen', nombreMessages: 4, slaDepasse: false },
  { reference: 'TCK-4655', sujet: 'Export CSV tronqué au-delà de 5 000 lignes', statut: 'RESOLU', priorite: 'NORMALE', categorie: 'ANOMALIE', crmClientRef: 'CLI-0001', clientRaisonSociale: 'Transports Nord', assigneA: 'bob', creeLe: '2026-08-02T08:45:00Z', derniereActiviteLe: '2026-08-14T09:15:00Z', derniereActivitePar: 'Bob Lefevre', nombreMessages: 9, slaDepasse: false },
  { reference: 'TCK-4519', sujet: "Changement d'adresse de facturation", statut: 'FERME', priorite: 'BASSE', categorie: 'FACTURATION', crmClientRef: 'CLI-0001', clientRaisonSociale: 'Transports Nord', assigneA: 'carol', creeLe: '2026-07-21T11:00:00Z', derniereActiviteLe: '2026-07-28T11:03:00Z', derniereActivitePar: 'système', nombreMessages: 2, slaDepasse: false },
  { reference: 'TCK-4488', sujet: 'Double prélèvement sur juin', statut: 'FERME', priorite: 'NORMALE', categorie: 'FACTURATION', crmClientRef: 'CLI-0001', clientRaisonSociale: 'Transports Nord', assigneA: 'bob', creeLe: '2026-07-03T09:30:00Z', derniereActiviteLe: '2026-07-09T16:20:00Z', derniereActivitePar: 'Carol Nguyen', nombreMessages: 5, slaDepasse: false },
  { reference: 'TCK-4402', sujet: 'Relance sur le devis de renouvellement', statut: 'FERME', priorite: 'BASSE', categorie: 'AUTRE', crmClientRef: 'CLI-0001', clientRaisonSociale: 'Transports Nord', assigneA: 'carol', creeLe: '2026-06-18T15:12:00Z', derniereActiviteLe: '2026-06-30T10:05:00Z', derniereActivitePar: 'système', nombreMessages: 3, slaDepasse: false },
  { reference: 'TCK-4377', sujet: 'Certificat expiré sur le portail de suivi', statut: 'RESOLU', priorite: 'HAUTE', categorie: 'ANOMALIE', crmClientRef: 'CLI-0001', clientRaisonSociale: 'Transports Nord', assigneA: 'bob', creeLe: '2026-06-05T07:50:00Z', derniereActiviteLe: '2026-06-06T12:30:00Z', derniereActivitePar: 'Bob Lefevre', nombreMessages: 6, slaDepasse: false },

  // --- Tickets du client CLI-0002 (david) : invisibles pour alice. C'est la cible BOLA. ---
  { reference: 'TCK-4818', sujet: 'Relance : avoir non reçu après annulation', statut: 'EN_COURS', priorite: 'NORMALE', categorie: 'FACTURATION', crmClientRef: 'CLI-0002', clientRaisonSociale: 'Ateliers Sud', assigneA: 'bob', creeLe: '2026-08-23T16:40:00Z', derniereActiviteLe: '2026-08-29T07:49:00Z', derniereActivitePar: 'Bob Lefevre', nombreMessages: 4, slaDepasse: false },
  { reference: 'TCK-4771', sujet: 'Modification du RIB de prélèvement', statut: 'EN_COURS', priorite: 'NORMALE', categorie: 'FACTURATION', crmClientRef: 'CLI-0002', clientRaisonSociale: 'Ateliers Sud', assigneA: 'carol', creeLe: '2026-08-17T09:05:00Z', derniereActiviteLe: '2026-08-28T18:07:00Z', derniereActivitePar: 'Carol Nguyen', nombreMessages: 5, slaDepasse: false },
  { reference: 'TCK-4690', sujet: 'Erreur 500 à la validation du panier', statut: 'RESOLU', priorite: 'HAUTE', categorie: 'ANOMALIE', crmClientRef: 'CLI-0002', clientRaisonSociale: 'Ateliers Sud', assigneA: 'bob', creeLe: '2026-08-09T13:22:00Z', derniereActiviteLe: '2026-08-12T10:11:00Z', derniereActivitePar: 'Bob Lefevre', nombreMessages: 8, slaDepasse: false },

  // --- Autres comptes : ils peuplent la file agent, jamais le portail client ---
  { reference: 'TCK-4402-B', sujet: 'Prélèvement en double sur le contrat annuel', statut: 'OUVERT', priorite: 'HAUTE', categorie: 'FACTURATION', crmClientRef: 'CLI-0004', clientRaisonSociale: 'Groupe Lauziere', assigneA: null, creeLe: '2026-08-26T08:00:00Z', derniereActiviteLe: '2026-08-26T08:00:00Z', derniereActivitePar: null, nombreMessages: 1, slaDepasse: true },
  { reference: 'TCK-4805', sujet: 'Demande de duplicata de facture 2025', statut: 'OUVERT', priorite: 'NORMALE', categorie: 'FACTURATION', crmClientRef: 'CLI-0003', clientRaisonSociale: 'Atelier Vernet', assigneA: 'carol', creeLe: '2026-08-21T10:15:00Z', derniereActiviteLe: '2026-08-28T09:30:00Z', derniereActivitePar: 'Carol Nguyen', nombreMessages: 2, slaDepasse: false },
  { reference: 'TCK-4744', sujet: 'Le rapport mensuel arrive vide', statut: 'EN_COURS', priorite: 'NORMALE', categorie: 'ANOMALIE', crmClientRef: 'CLI-0005', clientRaisonSociale: 'Merieux et Fils', assigneA: 'bob', creeLe: '2026-08-14T11:47:00Z', derniereActiviteLe: '2026-08-27T15:02:00Z', derniereActivitePar: 'Bob Lefevre', nombreMessages: 6, slaDepasse: false },
  { reference: 'TCK-4701', sujet: 'Ajouter la TVA intracommunautaire au contrat', statut: 'EN_COURS', priorite: 'BASSE', categorie: 'EVOLUTION', crmClientRef: 'CLI-0006', clientRaisonSociale: 'Fromageries Bellart', assigneA: 'carol', creeLe: '2026-08-10T09:00:00Z', derniereActiviteLe: '2026-08-25T14:20:00Z', derniereActivitePar: 'Carol Nguyen', nombreMessages: 3, slaDepasse: false },
  { reference: 'TCK-4668', sujet: 'Connexion impossible depuis la mise à jour', statut: 'RESOLU', priorite: 'HAUTE', categorie: 'ACCES', crmClientRef: 'CLI-0007', clientRaisonSociale: 'Imprimerie Kessler', assigneA: 'bob', creeLe: '2026-08-06T07:30:00Z', derniereActiviteLe: '2026-08-08T16:45:00Z', derniereActivitePar: 'Bob Lefevre', nombreMessages: 5, slaDepasse: false },
  { reference: 'TCK-4620', sujet: 'Résiliation du contrat au 31/12', statut: 'FERME', priorite: 'NORMALE', categorie: 'AUTRE', crmClientRef: 'CLI-0008', clientRaisonSociale: 'Cartonnages Vasseur', assigneA: 'carol', creeLe: '2026-07-30T13:10:00Z', derniereActiviteLe: '2026-08-06T09:00:00Z', derniereActivitePar: 'système', nombreMessages: 4, slaDepasse: false },
  { reference: 'TCK-4590', sujet: 'Facture non conforme au bon de commande', statut: 'OUVERT', priorite: 'NORMALE', categorie: 'FACTURATION', crmClientRef: 'CLI-0004', clientRaisonSociale: 'Groupe Lauziere', assigneA: 'bob', creeLe: '2026-07-27T08:20:00Z', derniereActiviteLe: '2026-08-28T11:15:00Z', derniereActivitePar: 'Bob Lefevre', nombreMessages: 3, slaDepasse: false },
  { reference: 'TCK-4551', sujet: 'Le webhook de notification ne part plus', statut: 'EN_COURS', priorite: 'HAUTE', categorie: 'ANOMALIE', crmClientRef: 'CLI-0003', clientRaisonSociale: 'Atelier Vernet', assigneA: null, creeLe: '2026-07-24T15:35:00Z', derniereActiviteLe: '2026-08-22T10:00:00Z', derniereActivitePar: null, nombreMessages: 2, slaDepasse: true },
  { reference: 'TCK-4503', sujet: 'Créer un accès en lecture seule pour la compta', statut: 'RESOLU', priorite: 'BASSE', categorie: 'ACCES', crmClientRef: 'CLI-0005', clientRaisonSociale: 'Merieux et Fils', assigneA: 'carol', creeLe: '2026-07-19T09:45:00Z', derniereActiviteLe: '2026-07-23T14:30:00Z', derniereActivitePar: 'Carol Nguyen', nombreMessages: 4, slaDepasse: false },
];

export const TICKETS: TicketResume[] = GRAINES.map((g, index) => ({
  id: index + 1,
  reference: g.reference,
  sujet: g.sujet,
  statut: g.statut,
  priorite: g.priorite,
  crmClientRef: g.crmClientRef,
  clientRaisonSociale: g.clientRaisonSociale,
  assigneA: g.assigneA ? (AGENT_PAR_USERNAME.get(g.assigneA) ?? null) : null,
  creeLe: g.creeLe,
  derniereActiviteLe: g.derniereActiviteLe,
  derniereActivitePar: g.derniereActivitePar,
  nombreMessages: g.nombreMessages,
  slaDepasse: g.slaDepasse,
}));

/** Fil du ticket TCK-4821 — celui des écrans 02 et 06 de la maquette. */
const COMMENTAIRES_4821: Commentaire[] = [
  {
    id: 1,
    auteurUsername: 'alice',
    auteurNom: 'Alice Durand',
    auteurType: 'CLIENT',
    contenu:
      "Bonjour, depuis ce matin le bouton de téléchargement de la facture de mars ne répond plus. Les factures d'avril et mai se téléchargent normalement.\n\nJ'ai essayé sur Chrome et Firefox, même résultat. Aucun message d'erreur n'apparaît.",
    creeLe: '2026-08-24T09:12:00Z',
    visibilite: 'PUBLIC',
  },
  {
    id: 2,
    auteurUsername: 'carol',
    auteurNom: 'Carol Nguyen',
    auteurType: 'AGENT',
    contenu:
      'Documents antérieurs à la migration du 12 mars : le lien signé pointe vers l\'ancien bucket. Ticket infra INF-217 ouvert. Ne pas mentionner la migration au client, la communication officielle part vendredi.',
    creeLe: '2026-08-24T09:58:00Z',
    visibilite: 'INTERNE',
  },
  {
    id: 3,
    auteurUsername: 'bob',
    auteurNom: 'Bob Lefevre',
    auteurType: 'AGENT',
    contenu:
      "Bonjour Alice, merci pour ces précisions. Je reproduis le problème de mon côté sur la facture de mars uniquement. Je la régénère et vous reviens aujourd'hui. En attendant, souhaitez-vous que je vous l'envoie par courriel ?",
    creeLe: '2026-08-24T10:38:00Z',
    visibilite: 'PUBLIC',
  },
  {
    id: 4,
    auteurUsername: 'bob',
    auteurNom: 'Bob Lefevre',
    auteurType: 'AGENT',
    contenu:
      "Régénération lancée, INF-217 toujours en attente côté infra. Si rien lundi, j'envoie le PDF à la main et je bascule le ticket sur Carol pour le suivi.",
    creeLe: '2026-08-28T16:20:00Z',
    visibilite: 'INTERNE',
  },
  {
    id: 5,
    auteurUsername: 'alice',
    auteurNom: 'Alice Durand',
    auteurType: 'CLIENT',
    contenu: 'Oui, par courriel ce serait parfait. Merci.',
    creeLe: '2026-08-29T08:26:00Z',
    visibilite: 'PUBLIC',
  },
];

const EVENEMENTS_4821: EvenementTicket[] = [
  { id: 1, type: 'NOTE_INTERNE', auteurNom: 'Bob Lefevre', detail: 'note interne ajoutée', creeLe: '2026-08-28T16:20:00Z' },
  { id: 2, type: 'CHANGEMENT_PRIORITE', auteurNom: 'Bob Lefevre', detail: 'priorité passée à Normale', creeLe: '2026-08-24T10:41:00Z' },
  { id: 3, type: 'ASSIGNATION', auteurNom: 'Carol Nguyen', detail: 'assigné à Bob Lefevre', creeLe: '2026-08-24T09:20:00Z' },
  { id: 4, type: 'CREATION', auteurNom: 'Alice Durand', detail: 'ticket créé par le client', creeLe: '2026-08-24T09:12:00Z' },
];

const DESCRIPTIONS: Record<string, string> = {
  'TCK-4821':
    "Depuis ce matin le bouton de téléchargement de la facture de mars ne répond plus. Les factures d'avril et mai se téléchargent normalement.",
};

/**
 * Détail d'un ticket.
 *
 * `pourAgent` décide si les commentaires internes et le journal sont **inclus**.
 * Volontairement filtré ici, à la source, et non masqué à l'affichage : c'est ce que
 * fera le backend au lot 4, et le composant de fil ne doit jamais recevoir une note
 * interne destinée à un client.
 */
export function detailFixture(id: number, pourAgent: boolean): TicketDetail | null {
  const resume = TICKETS.find((t) => t.id === id);
  if (!resume) {
    return null;
  }

  const tousLesCommentaires =
    resume.reference === 'TCK-4821' ? COMMENTAIRES_4821 : commentairesGeneriques(resume.id);

  return {
    ...resume,
    description: DESCRIPTIONS[resume.reference] ?? `Description du ticket ${resume.reference}.`,
    categorie: GRAINES[resume.id - 1].categorie,
    commentaires: pourAgent
      ? tousLesCommentaires
      : tousLesCommentaires.filter((c) => c.visibilite === 'PUBLIC'),
    evenements: pourAgent ? (resume.reference === 'TCK-4821' ? EVENEMENTS_4821 : []) : [],
  };
}

function commentairesGeneriques(idTicket: number): Commentaire[] {
  return [
    {
      id: idTicket * 100 + 1,
      auteurUsername: 'alice',
      auteurNom: 'Alice Durand',
      auteurType: 'CLIENT',
      contenu: 'Bonjour, pourriez-vous regarder ce point ? Merci d\'avance.',
      creeLe: '2026-08-20T09:00:00Z',
      visibilite: 'PUBLIC',
    },
    {
      id: idTicket * 100 + 2,
      auteurUsername: 'bob',
      auteurNom: 'Bob Lefevre',
      auteurType: 'AGENT',
      contenu: 'Bonjour, je prends en charge et je reviens vers vous rapidement.',
      creeLe: '2026-08-20T11:30:00Z',
      visibilite: 'PUBLIC',
    },
    {
      id: idTicket * 100 + 3,
      auteurUsername: 'carol',
      auteurNom: 'Carol Nguyen',
      auteurType: 'AGENT',
      contenu: 'À vérifier avec l\'équipe infra avant de répondre — ne pas promettre de délai.',
      creeLe: '2026-08-20T11:45:00Z',
      visibilite: 'INTERNE',
    },
  ];
}
