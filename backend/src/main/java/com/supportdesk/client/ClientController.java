package com.supportdesk.client;

import java.util.List;

import com.supportdesk.securite.UtilisateurCourant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Façade REST du CRM legacy.
 *
 * <p>Le front ne parle pas SOAP, et ne doit pas avoir à le faire : c'est tout l'intérêt du
 * motif. Le WSDL n'est jamais exposé, la forme du contrat du fournisseur ne fuit pas.
 *
 * <h2>Autorisation</h2>
 *
 * <p>{@code GET /api/clients/{ref}} est ouvert à un CLIENT <b>pour sa seule référence</b> :
 * elle est comparée au jeton avant tout appel au CRM. Sans cette comparaison, l'endpoint
 * serait une deuxième faille BOLA, sur un référentiel qui contient des SIRET et des
 * coordonnées de contact.
 *
 * <p>{@code GET /api/clients?recherche=} n'est pas ouvert aux CLIENT : le CRM ne se laisse
 * pas énumérer, et l'API ne doit pas offrir ce que le CRM refuse.
 */
@RestController
@RequestMapping("/api/clients")
public class ClientController {

	private final RepertoireClients repertoire;

	public ClientController(RepertoireClients repertoire) {
		this.repertoire = repertoire;
	}

	@GetMapping("/{clientRef}")
	public ClientCrm parReference(@PathVariable String clientRef, UtilisateurCourant utilisateur) {
		if (!utilisateur.estAgent() && !clientRef.equals(utilisateur.crmClientRef())) {
			throw new FicheClientRefuseeException();
		}
		return this.repertoire.parReference(clientRef);
	}

	@GetMapping
	public List<ClientCrm> rechercher(@RequestParam String recherche,
			UtilisateurCourant utilisateur) {
		if (!utilisateur.estAgent()) {
			throw new FicheClientRefuseeException();
		}
		return this.repertoire.rechercher(recherche);
	}
}
