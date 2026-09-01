package com.supportdesk.client;

import java.util.List;

import com.supportdesk.client.contrat.Client;
import com.supportdesk.client.contrat.GetClientRequest;
import com.supportdesk.client.contrat.GetClientResponse;
import com.supportdesk.client.contrat.SearchClientsRequest;
import com.supportdesk.client.contrat.SearchClientsResponse;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

/**
 * Consommation du CRM legacy.
 *
 * <h2>Traduction des faults</h2>
 *
 * <p>Un fault SOAP n'est pas une panne : c'est une réponse que le contrat prévoit. Le
 * laisser remonter tel quel ferait passer une règle métier pour un incident, et exposerait
 * au passage la forme interne du système d'en face.
 *
 * <pre>
 *   CLIENT_INCONNU        -&gt; 404   (la référence n'existe pas)
 *   CRITERE_OBLIGATOIRE   -&gt; 400   (la recherche a besoin d'un motif)
 *   timeout, connexion    -&gt; 503   (le CRM ne répond pas)
 * </pre>
 *
 * <h2>Cache</h2>
 *
 * <p>Chaque appel coûte 400 ms. Une file de cinquante tickets touchant cinq comptes ferait
 * cinquante appels — vingt secondes — pour cinq réponses distinctes. Le cache ramène à cinq.
 * Une identité de société ne change pas dans la minute : le compromis est franc.
 */
@Component
public class CrmClientSoap {

	private final WebServiceTemplate template;

	public CrmClientSoap(WebServiceTemplate webServiceTemplateCrm) {
		this.template = webServiceTemplateCrm;
	}

	@Cacheable(cacheNames = "clientsCrm", unless = "#result == null")
	public ClientCrm parReference(String clientRef) {
		GetClientRequest requete = new GetClientRequest();
		requete.setClientRef(clientRef);

		try {
			GetClientResponse reponse = (GetClientResponse) this.template
					.marshalSendAndReceive(requete);
			return traduire(reponse.getClient());
		}
		catch (SoapFaultClientException ex) {
			throw traduireFault(ex, clientRef);
		}
		catch (WebServiceIOException ex) {
			throw new CrmIndisponibleException("Le référentiel clients ne répond pas", ex);
		}
	}

	public List<ClientCrm> rechercher(String motif) {
		if (motif == null || motif.isBlank()) {
			// Le CRM refuserait de toute façon : autant ne pas dépenser 400 ms pour
			// l'apprendre, et donner au client une erreur immédiate et exacte.
			throw new CritereRechercheManquantException();
		}

		SearchClientsRequest requete = new SearchClientsRequest();
		requete.setNamePattern(motif);

		try {
			SearchClientsResponse reponse = (SearchClientsResponse) this.template
					.marshalSendAndReceive(requete);
			return reponse.getClient().stream().map(CrmClientSoap::traduire).toList();
		}
		catch (SoapFaultClientException ex) {
			throw traduireFault(ex, null);
		}
		catch (WebServiceIOException ex) {
			throw new CrmIndisponibleException("Le référentiel clients ne répond pas", ex);
		}
	}

	/**
	 * Traduit un fault en exception du domaine.
	 *
	 * <p>Le code de fault du CRM est porté par le {@code faultstring} — c'est ainsi que ce
	 * système l'a toujours fait. On s'y adapte plutôt que de demander au fournisseur de
	 * changer son contrat.
	 */
	private static RuntimeException traduireFault(SoapFaultClientException ex, String clientRef) {
		String raison = (ex.getFaultStringOrReason() != null) ? ex.getFaultStringOrReason() : "";

		if (raison.contains("CLIENT_INCONNU")) {
			return new ClientCrmInconnuException(clientRef);
		}
		if (raison.contains("CRITERE_OBLIGATOIRE")) {
			return new CritereRechercheManquantException();
		}
		// Un fault non prévu par le contrat : on ne le devine pas, on le signale comme une
		// indisponibilité plutôt que de le laisser passer pour une réponse valide.
		return new CrmIndisponibleException("Réponse inattendue du référentiel clients : " + raison, ex);
	}

	private static ClientCrm traduire(Client client) {
		return new ClientCrm(client.getClientRef(), client.getRaisonSociale(), client.getSiret(),
				client.getContactEmail(), client.getContactTel(), client.isActif());
	}
}
