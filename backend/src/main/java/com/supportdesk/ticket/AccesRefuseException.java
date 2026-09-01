package com.supportdesk.ticket;

/**
 * L'appelant est authentifié mais le ticket ne lui appartient pas.
 *
 * <p>403 et non 404 : la maquette prévoit un écran qui nomme le compte propriétaire, ce qui
 * suppose d'admettre l'existence du ticket. Le choix est assumé et documenté — il divulgue
 * qu'un identifiant existe. L'alternative (404 indifférencié) empêche l'énumération mais
 * rend l'écran d'aide impossible. Pour un portail de support entre professionnels, la
 * lisibilité l'emporte ; pour une application grand public, ce serait l'inverse.
 */
public class AccesRefuseException extends RuntimeException {

	private final Long ticketId;

	public AccesRefuseException(Long ticketId) {
		super("Ce ticket appartient à un autre compte");
		this.ticketId = ticketId;
	}

	public Long getTicketId() {
		return this.ticketId;
	}
}
