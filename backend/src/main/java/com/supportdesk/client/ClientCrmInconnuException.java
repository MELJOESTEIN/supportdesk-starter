package com.supportdesk.client;

/**
 * Le CRM a répondu par un fault {@code CLIENT_INCONNU}.
 *
 * <p>Un fault SOAP n'est pas une panne : c'est une réponse. Le laisser remonter en 500
 * ferait passer une règle métier pour un incident.
 */
public class ClientCrmInconnuException extends RuntimeException {

	private final String clientRef;

	public ClientCrmInconnuException(String clientRef) {
		super("Aucune fiche client pour la référence " + clientRef);
		this.clientRef = clientRef;
	}

	public String getClientRef() {
		return this.clientRef;
	}
}
