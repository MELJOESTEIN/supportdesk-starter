package fr.acme.legacy.crm;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

/**
 * Référence inconnue : le service renvoie un Fault, jamais une réponse vide.
 * Comportement d'origine conservé — l'appelant doit traduire ce Fault, pas le propager.
 */
@SoapFault(faultCode = FaultCode.CLIENT, faultStringOrReason = "CLIENT_INCONNU")
public class ClientInconnuException extends RuntimeException {

	public ClientInconnuException(String clientRef) {
		super("CLIENT_INCONNU: aucune fiche pour la reference " + clientRef);
	}
}
