package com.supportdesk.tableaudebord;

import java.time.Instant;
import java.util.List;

import com.supportdesk.ticket.Ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Agrégats du tableau de bord.
 *
 * <p>Tous les comptages sont faits par la base. Charger les tickets pour les compter en
 * mémoire fonctionnerait sur le jeu de démonstration et s'écroulerait à 200 000 lignes.
 */
public interface TableauDeBordRepository extends JpaRepository<Ticket, Long> {

	@Query("SELECT t.statut, COUNT(t) FROM Ticket t GROUP BY t.statut")
	List<Object[]> compterParStatut();

	@Query("""
			SELECT COUNT(t) FROM Ticket t
			WHERE t.statut = com.supportdesk.ticket.StatutTicket.RESOLU
			  AND t.derniereActiviteLe >= :depuis
			""")
	long compterResolusDepuis(@Param("depuis") Instant depuis);

	@Query("""
			SELECT COUNT(t) FROM Ticket t
			WHERE t.premiereReponseLe IS NULL AND t.creeLe < :avant
			""")
	long compterSansReponseAvant(@Param("avant") Instant avant);

	@Query("""
			SELECT COUNT(t) FROM Ticket t
			WHERE t.premiereReponseLe IS NOT NULL AND t.echeanceSlaLe IS NOT NULL
			  AND t.premiereReponseLe <= t.echeanceSlaLe
			""")
	long compterSlaRespecte();

	@Query("SELECT COUNT(t) FROM Ticket t WHERE t.premiereReponseLe IS NOT NULL")
	long compterAvecPremiereReponse();

	@Query("""
			SELECT t.creeLe, t.premiereReponseLe FROM Ticket t
			WHERE t.premiereReponseLe IS NOT NULL
			""")
	List<Object[]> delaisDePremiereReponse();

	@Query(value = """
			SELECT jour::date AS jour,
			       (SELECT COUNT(*) FROM ticket t WHERE t.cree_le::date = jour::date) AS crees,
			       (SELECT COUNT(*) FROM ticket t
			         WHERE t.statut IN ('RESOLU', 'FERME') AND t.derniere_activite_le::date = jour::date) AS resolus
			FROM generate_series(:debut, :fin, INTERVAL '1 day') AS jour
			ORDER BY jour
			""", nativeQuery = true)
	List<Object[]> activiteParJour(@Param("debut") Instant debut, @Param("fin") Instant fin);
}
