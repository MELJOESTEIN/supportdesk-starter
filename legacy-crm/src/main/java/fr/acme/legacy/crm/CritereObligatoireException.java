package fr.acme.legacy.crm;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

/**
 * Recherche sans critère : refusée. Le référentiel ne se laisse pas énumérer.
 */
@SoapFault(faultCode = FaultCode.CLIENT, faultStringOrReason = "CRITERE_OBLIGATOIRE")
public class CritereObligatoireException extends RuntimeException {

	public CritereObligatoireException() {
		super("CRITERE_OBLIGATOIRE: le motif de recherche est obligatoire");
	}
}
