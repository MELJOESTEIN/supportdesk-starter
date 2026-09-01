package com.supportdesk.ticket;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

	/**
	 * Les comptes clients présents dans la file, sans doublon.
	 *
	 * <p>Sert à construire le filtre « Client » du back-office. La liste vient des tickets
	 * et non du CRM : le CRM refuse volontairement d'être énuméré (voir
	 * {@code ClientController}), et une option de filtre qui ne correspond à aucun ticket
	 * n'a de toute façon aucun intérêt.
	 */
	@Query("SELECT DISTINCT t.crmClientRef FROM Ticket t ORDER BY t.crmClientRef")
	java.util.List<String> referencesClientsDeLaFile();

	/**
	 * Liste paginée.
	 *
	 * <p>Le périmètre ({@code crmClientRef}) est un paramètre de la requête, pas un filtre
	 * appliqué après coup : les lignes des autres comptes ne sont jamais lues. Quand il est
	 * vide, l'appelant est un agent et voit tout — c'est le service qui décide, à partir du
	 * jeton, jamais le client.
	 *
	 * <p>Aucun paramètre n'est {@code null} : un paramètre nul est non typé, et PostgreSQL
	 * le prend pour un {@code bytea}, ce qui fait échouer {@code LOWER()}. La chaîne vide
	 * joue le rôle d'« absence de filtre ».
	 *
	 * <p>{@code inclureInternes} change le <b>comptage</b> des messages : un client ne doit
	 * pas déduire l'existence de notes internes d'un compteur trop élevé.
	 */
	@Query(value = """
			SELECT new com.supportdesk.ticket.LigneTicket(
			    t.id, t.reference, t.sujet, t.statut, t.priorite, t.crmClientRef,
			    a.username, a.nomComplet, a.niveau, a.equipe,
			    t.creeLe, t.derniereActiviteLe, t.derniereActivitePar, da.nomComplet,
			    (SELECT COUNT(c) FROM Commentaire c
			      WHERE c.ticket = t
			        AND (:inclureInternes = TRUE OR c.visibilite = com.supportdesk.ticket.VisibiliteCommentaire.PUBLIC)),
			    CASE WHEN t.premiereReponseLe IS NULL AND t.echeanceSlaLe IS NOT NULL
			              AND t.echeanceSlaLe < :maintenant
			         THEN TRUE ELSE FALSE END)
			FROM Ticket t
			LEFT JOIN t.assigneA a
			LEFT JOIN com.supportdesk.agent.Agent da ON da.username = t.derniereActivitePar
			WHERE (:crmClientRef = '' OR t.crmClientRef = :crmClientRef)
			  AND t.statut IN :statuts
			  AND (:assigneA = '' OR a.username = :assigneA)
			  AND (:nonAssigne = FALSE OR a.username IS NULL)
			  AND (:recherche = ''
			       OR LOWER(t.sujet) LIKE LOWER(CONCAT('%', :recherche, '%'))
			       OR LOWER(t.reference) LIKE LOWER(CONCAT('%', :recherche, '%')))
			""",
			countQuery = """
			SELECT COUNT(t) FROM Ticket t
			LEFT JOIN t.assigneA a
			WHERE (:crmClientRef = '' OR t.crmClientRef = :crmClientRef)
			  AND t.statut IN :statuts
			  AND (:assigneA = '' OR a.username = :assigneA)
			  AND (:nonAssigne = FALSE OR a.username IS NULL)
			  AND (:recherche = ''
			       OR LOWER(t.sujet) LIKE LOWER(CONCAT('%', :recherche, '%'))
			       OR LOWER(t.reference) LIKE LOWER(CONCAT('%', :recherche, '%')))
			""")
	Page<LigneTicket> lister(@Param("crmClientRef") String crmClientRef,
			@Param("statuts") java.util.List<StatutTicket> statuts,
			@Param("assigneA") String assigneA,
			@Param("nonAssigne") boolean nonAssigne,
			@Param("recherche") String recherche,
			@Param("inclureInternes") boolean inclureInternes,
			@Param("maintenant") Instant maintenant,
			Pageable pageable);

	/**
	 * Détail d'un ticket avec son agent.
	 *
	 * <p>Les commentaires ne sont pas dans ce graphe : ils sont chargés par une requête
	 * dédiée, filtrée par visibilité. Les ramener ici obligerait à filtrer en mémoire, donc
	 * à charger des notes internes pour un client — exactement ce qu'on veut éviter.
	 */
	@EntityGraph(attributePaths = "assigneA")
	Optional<Ticket> findWithAgentById(Long id);

	Optional<Ticket> findByReference(String reference);
}
