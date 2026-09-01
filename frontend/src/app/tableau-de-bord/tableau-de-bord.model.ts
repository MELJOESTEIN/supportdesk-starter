import { StatutTicket } from '../ticket/ticket.model';

/** Une des six tuiles de l'écran 04. */
export interface Indicateur {
  cle:
    | 'OUVERTS'
    | 'EN_COURS'
    | 'RESOLUS_7J'
    | 'PREMIERE_REPONSE_MEDIANE'
    | 'SLA_RESPECTE'
    | 'SANS_REPONSE_48H';
  libelle: string;
  valeur: string;
  precision: string;
  alerte: boolean;
}

/** Une barre du graphe « activité des 14 derniers jours ». */
export interface ActiviteJour {
  jour: string;
  crees: number;
  resolus: number;
}

export interface RepartitionStatut {
  statut: StatutTicket;
  nombre: number;
}

export interface TableauDeBord {
  arreteLe: string;
  equipe: string;
  indicateurs: Indicateur[];
  activite: ActiviteJour[];
  repartition: RepartitionStatut[];
  totalPeriode: number;
}
