package fr.acme.legacy.crm;

import javax.xml.transform.Source;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webservices.test.autoconfigure.server.WebServiceServerTest;
import org.springframework.context.annotation.Import;
import org.springframework.ws.test.server.MockWebServiceClient;
import org.springframework.xml.transform.StringSource;

import static org.springframework.ws.test.server.RequestCreators.withPayload;
import static org.springframework.ws.test.server.ResponseMatchers.clientOrSenderFault;
import static org.springframework.ws.test.server.ResponseMatchers.xpath;

/**
 * Le paquet d'autoconfiguration de test a changé en Boot 4 :
 * org.springframework.boot.webservices.test.autoconfigure.server (et non
 * org.springframework.boot.test.autoconfigure.webservices.server comme en Boot 3).
 */
@WebServiceServerTest(CrmClientsEndpoint.class)
@Import({ RepertoireClients.class, LatenceLegacy.class })
class CrmClientsEndpointTests {

	private static final String NS = "http://legacy.acme.fr/crm";

	@Autowired
	private MockWebServiceClient client;

	@Test
	void getClient_referenceConnue_renvoieLaRaisonSociale() {
		Source requete = new StringSource("""
				<GetClientRequest xmlns="http://legacy.acme.fr/crm">
				  <clientRef>CLI-0001</clientRef>
				</GetClientRequest>
				""");

		this.client.sendRequest(withPayload(requete))
				.andExpect(xpath("//crm:GetClientResponse/crm:client/crm:raisonSociale", namespaces())
						.evaluatesTo("Transports Nord"))
				.andExpect(xpath("//crm:GetClientResponse/crm:client/crm:siret", namespaces())
						.evaluatesTo("48291736500017"));
	}

	@Test
	void getClient_referenceInconnue_renvoieUnFault() {
		Source requete = new StringSource("""
				<GetClientRequest xmlns="http://legacy.acme.fr/crm">
				  <clientRef>CLI-9999</clientRef>
				</GetClientRequest>
				""");

		// Le contrat impose un Fault, pas une réponse vide : c'est ce que l'appelant devra traduire.
		this.client.sendRequest(withPayload(requete))
				.andExpect(clientOrSenderFault("CLIENT_INCONNU"));
	}

	@Test
	void searchClients_critereVide_renvoieFaultCritereObligatoire() {
		Source requete = new StringSource("""
				<SearchClientsRequest xmlns="http://legacy.acme.fr/crm">
				  <namePattern></namePattern>
				</SearchClientsRequest>
				""");

		this.client.sendRequest(withPayload(requete))
				.andExpect(clientOrSenderFault("CRITERE_OBLIGATOIRE"));
	}

	@Test
	void searchClients_motif_filtreSurLaRaisonSociale() {
		Source requete = new StringSource("""
				<SearchClientsRequest xmlns="http://legacy.acme.fr/crm">
				  <namePattern>atelier</namePattern>
				</SearchClientsRequest>
				""");

		// « Ateliers Sud » et « Atelier Vernet », insensible à la casse : deux résultats, pas plus.
		this.client.sendRequest(withPayload(requete))
				.andExpect(xpath("count(//crm:SearchClientsResponse/crm:client)", namespaces()).evaluatesTo(2));
	}

	private static java.util.Map<String, String> namespaces() {
		return java.util.Map.of("crm", NS);
	}
}
