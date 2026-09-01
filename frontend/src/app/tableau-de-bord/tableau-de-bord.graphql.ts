/**
 * Requête du tableau de bord agent.
 *
 * <p>Une seule requête pour six indicateurs, quatorze jours d'activité et la répartition
 * par statut — là où REST en demanderait trois, ou obligerait à un endpoint sur mesure de
 * plus. C'est exactement le cas où GraphQL gagne : un écran de back-office qui compose sa
 * propre vue.
 */
export const REQUETE_TABLEAU_DE_BORD = `
  query TableauDeBord($jours: Int!) {
    tableauDeBord(jours: $jours) {
      arreteLe
      equipe
      totalPeriode
      indicateurs { cle libelle valeur precision alerte }
      activite { jour crees resolus }
      repartition { statut nombre }
    }
  }
`;
