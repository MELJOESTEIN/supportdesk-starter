package com.supportdesk.ticket;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {

	/**
	 * Commentaires d'un ticket, filtrés par visibilité <b>dans la requête</b>.
	 *
	 * <p>C'est le point le plus important du domaine : pour un client, les notes internes ne
	 * sont pas chargées du tout. Elles ne peuvent donc ni être sérialisées par erreur, ni
	 * apparaître dans un journal, ni fuir par une réponse d'erreur.
	 */
	@Query("""
			SELECT c FROM Commentaire c
			WHERE c.ticket.id = :ticketId
			  AND (:inclureInternes = TRUE OR c.visibilite = com.supportdesk.ticket.VisibiliteCommentaire.PUBLIC)
			ORDER BY c.creeLe ASC, c.id ASC
			""")
	List<Commentaire> parTicket(@Param("ticketId") Long ticketId,
			@Param("inclureInternes") boolean inclureInternes);
}
