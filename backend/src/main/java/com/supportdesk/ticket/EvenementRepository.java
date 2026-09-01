package com.supportdesk.ticket;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EvenementRepository extends JpaRepository<EvenementTicket, Long> {

	/**
	 * Le journal n'est jamais servi à un client : le service ne l'appelle que pour un agent.
	 *
	 * <p>Départage par identifiant, et ce n'est pas une précaution théorique :
	 * {@code TicketService#modifier} journalise le changement de statut, celui de priorité et
	 * l'assignation avec le <b>même</b> {@code Instant}. Trois événements ex æquo à chaque
	 * modification un peu large. Sans second critère, Postgres les rend dans l'ordre qui
	 * l'arrange, qui change avec le plan d'exécution — un journal qui se réordonne tout seul
	 * entre deux consultations.
	 */
	List<EvenementTicket> findByTicketIdOrderByCreeLeDescIdDesc(Long ticketId);
}
