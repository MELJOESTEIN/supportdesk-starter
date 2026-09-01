package com.supportdesk.ticket;

/** Ticket absent. Se traduit en 404 ProblemDetail, jamais en trace. */
public class TicketIntrouvableException extends RuntimeException {

	private final Long id;

	public TicketIntrouvableException(Long id) {
		super("Aucun ticket avec l'identifiant " + id);
		this.id = id;
	}

	public Long getId() {
		return this.id;
	}
}
