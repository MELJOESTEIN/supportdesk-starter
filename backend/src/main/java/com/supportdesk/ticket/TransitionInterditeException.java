package com.supportdesk.ticket;

/** Transition de statut refusée par le cycle de vie. Se traduit en 409. */
public class TransitionInterditeException extends RuntimeException {

	private final StatutTicket depuis;

	private final StatutTicket vers;

	public TransitionInterditeException(StatutTicket depuis, StatutTicket vers) {
		super("Transition interdite de " + depuis + " vers " + vers);
		this.depuis = depuis;
		this.vers = vers;
	}

	public StatutTicket getDepuis() {
		return this.depuis;
	}

	public StatutTicket getVers() {
		return this.vers;
	}
}
