package fr.acme.legacy.crm;

import java.util.List;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import fr.acme.legacy.crm.contrat.Client;
import fr.acme.legacy.crm.contrat.GetClientRequest;
import fr.acme.legacy.crm.contrat.GetClientResponse;
import fr.acme.legacy.crm.contrat.SearchClientsRequest;
import fr.acme.legacy.crm.contrat.SearchClientsResponse;

@Endpoint
public class CrmClientsEndpoint {

	static final String NAMESPACE = "http://legacy.acme.fr/crm";

	private final RepertoireClients repertoire;

	private final LatenceLegacy latence;

	public CrmClientsEndpoint(RepertoireClients repertoire, LatenceLegacy latence) {
		this.repertoire = repertoire;
		this.latence = latence;
	}

	@PayloadRoot(namespace = NAMESPACE, localPart = "GetClientRequest")
	@ResponsePayload
	public GetClientResponse getClient(@RequestPayload GetClientRequest requete) {
		this.latence.patienter();
		Client client = this.repertoire.parReference(requete.getClientRef())
				.orElseThrow(() -> new ClientInconnuException(requete.getClientRef()));

		GetClientResponse reponse = new GetClientResponse();
		reponse.setClient(client);
		return reponse;
	}

	@PayloadRoot(namespace = NAMESPACE, localPart = "SearchClientsRequest")
	@ResponsePayload
	public SearchClientsResponse searchClients(@RequestPayload SearchClientsRequest requete) {
		this.latence.patienter();
		String motif = requete.getNamePattern();
		if (motif == null || motif.isBlank()) {
			throw new CritereObligatoireException();
		}

		List<Client> trouves = this.repertoire.parMotif(motif);
		SearchClientsResponse reponse = new SearchClientsResponse();
		reponse.getClient().addAll(trouves);
		return reponse;
	}
}
