package com.supportdesk.ticket;

/**
 * Un agent tente de créer un ticket.
 *
 * <p>Le domaine est clair : « le Client déclare des tickets, l'Agent les traite ». Un agent
 * n'a pas de référence CRM — il n'appartient à aucun compte — et le ticket créé n'aurait
 * donc pas de propriétaire.
 *
 * <p>Trouvé par la revue OWASP : le cas produisait une erreur 500 sur violation de contrainte,
 * ce qui est la mauvaise façon de refuser. Un refus doit être une décision, pas un accident.
 */
public class CreationReserveeAuClientException extends RuntimeException {

	public CreationReserveeAuClientException() {
		super("Seul un client peut déclarer un ticket");
	}
}
