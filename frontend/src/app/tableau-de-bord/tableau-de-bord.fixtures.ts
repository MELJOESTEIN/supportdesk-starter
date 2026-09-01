import { TableauDeBord } from './tableau-de-bord.model';

/** Valeurs de l'écran 04 de la maquette. Remplacées par `/api/tableau-de-bord` au lot 3. */
export const TABLEAU_DE_BORD: TableauDeBord = {
  arreteLe: '2026-08-29T08:40:00Z',
  equipe: 'Facturation',
  indicateurs: [
    { cle: 'OUVERTS', libelle: '● TICKETS OUVERTS', valeur: '64', precision: '+6 depuis hier', alerte: false },
    { cle: 'EN_COURS', libelle: '◑ EN COURS', valeur: '38', precision: 'dont 12 assignés à vous', alerte: false },
    { cle: 'RESOLUS_7J', libelle: '✓ RÉSOLUS · 7 JOURS', valeur: '129', precision: '+11 % vs semaine préc.', alerte: false },
    { cle: 'PREMIERE_REPONSE_MEDIANE', libelle: '1RE RÉPONSE · MÉDIANE', valeur: '1 h 47', precision: 'objectif 2 h · tenu', alerte: false },
    { cle: 'SLA_RESPECTE', libelle: 'SLA RESPECTÉ', valeur: '94,2 %', precision: 'seuil contractuel 90 %', alerte: false },
    { cle: 'SANS_REPONSE_48H', libelle: '⚠ SANS RÉPONSE > 48 H', valeur: '9', precision: 'Voir la liste', alerte: true },
  ],
  activite: [
    { jour: '2026-08-16', crees: 52, resolus: 44 },
    { jour: '2026-08-17', crees: 61, resolus: 57 },
    { jour: '2026-08-18', crees: 38, resolus: 49 },
    { jour: '2026-08-19', crees: 27, resolus: 31 },
    { jour: '2026-08-20', crees: 71, resolus: 58 },
    { jour: '2026-08-21', crees: 84, resolus: 66 },
    { jour: '2026-08-22', crees: 66, resolus: 79 },
    { jour: '2026-08-23', crees: 58, resolus: 62 },
    { jour: '2026-08-24', crees: 47, resolus: 55 },
    { jour: '2026-08-25', crees: 33, resolus: 29 },
    { jour: '2026-08-26', crees: 76, resolus: 68 },
    { jour: '2026-08-27', crees: 92, resolus: 74 },
    { jour: '2026-08-28', crees: 69, resolus: 81 },
    { jour: '2026-08-29', crees: 55, resolus: 60 },
  ],
  repartition: [
    { statut: 'OUVERT', nombre: 64 },
    { statut: 'EN_COURS', nombre: 38 },
    { statut: 'RESOLU', nombre: 96 },
    { statut: 'FERME', nombre: 49 },
  ],
  totalPeriode: 247,
};
