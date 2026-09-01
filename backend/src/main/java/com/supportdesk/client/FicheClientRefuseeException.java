package com.supportdesk.client;

/** Consultation d'une fiche client qui n'est pas la sienne. */
public class FicheClientRefuseeException extends RuntimeException {

	public FicheClientRefuseeException() {
		super("Cette fiche client ne vous est pas accessible");
	}
}
