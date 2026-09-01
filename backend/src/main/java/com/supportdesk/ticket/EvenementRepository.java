package com.supportdesk.ticket;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EvenementRepository extends JpaRepository<EvenementTicket, Long> {

	/** Le journal n'est jamais servi à un client : le service ne l'appelle que pour un agent. */
	List<EvenementTicket> findByTicketIdOrderByCreeLeDesc(Long ticketId);
}
