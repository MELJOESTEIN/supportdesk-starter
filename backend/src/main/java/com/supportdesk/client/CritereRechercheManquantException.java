package com.supportdesk.client;

/** Le CRM refuse une recherche sans critère : il ne se laisse pas énumérer. */
public class CritereRechercheManquantException extends RuntimeException {

	public CritereRechercheManquantException() {
		super("Le motif de recherche est obligatoire");
	}
}
