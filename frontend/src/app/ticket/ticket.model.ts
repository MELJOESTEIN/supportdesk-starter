import { AgentResume } from '../agent/agent.model';

export type StatutTicket = 'OUVERT' | 'EN_COURS' | 'RESOLU' | 'FERME';

export type PrioriteTicket = 'BASSE' | 'NORMALE' | 'HAUTE';

export type CategorieTicket =
  | 'FACTURATION'
  | 'ACCES'
  | 'ANOMALIE'
  | 'EVOLUTION'
  | 'AUTRE';

/** Visible par tout le monde, ou réservée à l'équipe support. */
export type VisibiliteCommentaire = 'PUBLIC' | 'INTERNE';

export type AuteurType = 'CLIENT' | 'AGENT';

export type TypeEvenement =
  | 'CREATION'
  | 'ASSIGNATION'
  | 'CHANGEMENT_STATUT'
  | 'CHANGEMENT_PRIORITE'
  | 'REPONSE_CLIENT'
  | 'NOTE_INTERNE';

/** Ligne de tableau — écrans 01 (portail) et 05 (file agent). */
export interface TicketResume {
  id: number;
  reference: string;
  sujet: string;
  statut: StatutTicket;
  priorite: PrioriteTicket;
  /** Seule donnée d'identité client stockée en base. */
  crmClientRef: string;
  /** Lu dans le CRM legacy en SOAP. `null` si le référentiel ne répond pas. */
  clientRaisonSociale: string | null;
  assigneA: AgentResume | null;
  /** ISO-8601 : Jackson 3 n'écrit plus les dates en epoch. */
  creeLe: string;
  derniereActiviteLe: string;
  derniereActivitePar: string | null;
  nombreMessages: number;
  slaDepasse: boolean;
}

export interface Commentaire {
  id: number;
  auteurUsername: string;
  auteurNom: string;
  auteurType: AuteurType;
  contenu: string;
  creeLe: string;
  /**
   * `INTERNE` n'atteint jamais un client : le backend l'exclut de la requête,
   * ce champ n'est donc jamais à `INTERNE` dans une réponse destinée à un CLIENT.
   * Ne pas s'en servir pour masquer à l'affichage — ce serait le filtrer trop tard.
   */
  visibilite: VisibiliteCommentaire;
}

export interface EvenementTicket {
  id: number;
  type: TypeEvenement;
  auteurNom: string;
  detail: string;
  creeLe: string;
}

/** Écrans 02 (client) et 06 (agent). `evenements` est vide pour un CLIENT. */
export interface TicketDetail extends TicketResume {
  /**
   * Courriel de contact du compte, lu au CRM. `null` pour un CLIENT — non parce que ce
   * serait un secret, c'est son propre courriel, mais parce que son écran ne l'affiche pas.
   * Un champ qu'aucune interface ne montre n'a rien à faire sur le réseau.
   */
  clientContact: string | null;
  description: string;
  categorie: CategorieTicket;
  commentaires: Commentaire[];
  evenements: EvenementTicket[];
}

/**
 * Création d'un ticket.
 *
 * Ni `statut`, ni `crmClientRef`, ni `assigneA`, ni `priorite` : ces valeurs sont décidées
 * par le serveur. Les accepter ici serait de l'affectation en masse — un client s'assignerait
 * son propre ticket ou le déclarerait résolu.
 */
export interface NouveauTicket {
  sujet: string;
  categorie: CategorieTicket;
  description: string;
}

export interface NouveauCommentaire {
  contenu: string;
  /** Un CLIENT ne peut envoyer que `PUBLIC` ; le backend refuse `INTERNE` sans le rôle AGENT. */
  visibilite: VisibiliteCommentaire;
}

/** Mutations réservées aux agents (écran 06, encart TRAITEMENT). */
export interface ModificationTicket {
  statut?: StatutTicket;
  priorite?: PrioriteTicket;
  /** `null` = désassigner. */
  assigneA?: string | null;
}

/** Option du menu « Client » de la file agent. Aligné sur TicketDtos.OptionClient. */
export interface OptionClient {
  reference: string;
  raisonSociale: string;
}

export interface FiltreTickets {
  statuts?: StatutTicket[];
  crmClientRef?: string;
  assigneA?: string;
  recherche?: string;
  page?: number;
  taille?: number;
}
