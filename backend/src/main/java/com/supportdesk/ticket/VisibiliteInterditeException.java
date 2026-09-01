package com.supportdesk.ticket;

/** Un client tente d'écrire une note interne. Se traduit en 403. */
public class VisibiliteInterditeException extends RuntimeException {

	public VisibiliteInterditeException() {
		super("Seul un agent peut écrire une note interne");
	}
}
