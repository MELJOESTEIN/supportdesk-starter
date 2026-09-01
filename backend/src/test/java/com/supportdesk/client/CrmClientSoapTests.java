package com.supportdesk.client;

import java.util.List;
import java.util.Set;

import com.supportdesk.BaseIntegration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webservices.test.autoconfigure.client.AutoConfigureMockWebServiceServer;
import org.springframework.cache.CacheManager;
import org.springframework.ws.test.client.MockWebServiceServer;
import org.springframework.xml.transform.StringSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.ws.test.client.RequestMatchers.payload;
import static org.springframework.ws.test.client.ResponseCreators.withPayload;
import static org.springframework.ws.test.client.ResponseCreators.withClientOrSenderFault;
import static org.springframework.ws.test.client.ResponseCreators.withException;

/**
 * Consommation du CRM, avec un serveur SOAP simulé.
 *
 * <p>Le vrai CRM n'est pas nécessaire ici, et c'est voulu : ces tests doivent tourner en CI,
 * décrire précisément ce que le service fait d'un fault, et pouvoir simuler une panne — ce
 * qu'un conteneur en bonne santé ne sait pas faire.
 */
@SpringBootTest
@AutoConfigureMockWebServiceServer
class CrmClientSoapTests extends BaseIntegration {

	private static final String NS = "http://legacy.acme.fr/crm";

	@Autowired
	private MockWebServiceServer serveur;

	@Autowired
	private CrmClientSoap crm;

	@Autowired
	private RepertoireClients repertoire;

	@Autowired
	private CacheManager caches;

	@BeforeEach
	void viderLeCache() {
		// Sans ça, un test hérite des réponses mises en cache par le précédent et le
		// compteur d'appels ne veut plus rien dire.
		this.caches.getCacheNames().forEach((nom) -> this.caches.getCache(nom).clear());
	}

	@Test
	@DisplayName("une fiche connue est traduite en modèle du domaine")
	void ficheConnue_estTraduite() {
		this.serveur.expect(payload(new StringSource(requeteGetClient("CLI-0001"))))
				.andRespond(withPayload(new StringSource(reponseGetClient("CLI-0001", "Transports Nord"))));

		ClientCrm fiche = this.crm.parReference("CLI-0001");

		assertThat(fiche.raisonSociale()).isEqualTo("Transports Nord");
		assertThat(fiche.actif()).isTrue();
		this.serveur.verify();
	}

	@Test
	@DisplayName("un fault CLIENT_INCONNU devient une exception du domaine, pas une erreur technique")
	void faultClientInconnu_devientExceptionDuDomaine() {
		this.serveur.expect(payload(new StringSource(requeteGetClient("CLI-9999"))))
				.andRespond(withClientOrSenderFault("CLIENT_INCONNU", java.util.Locale.FRANCE));

		assertThatThrownBy(() -> this.crm.parReference("CLI-9999"))
				.isInstanceOf(ClientCrmInconnuException.class);
		this.serveur.verify();
	}

	@Test
	@DisplayName("un fault CRITERE_OBLIGATOIRE devient une erreur de requête")
	void faultCritereObligatoire_devientErreurDeRequete() {
		// Le service court-circuite un motif vide sans appeler le CRM : on éprouve donc le
		// cas où le CRM refuse un motif que le service, lui, jugeait acceptable.
		this.serveur.expect(payload(new StringSource(requeteSearch("??"))))
				.andRespond(withClientOrSenderFault("CRITERE_OBLIGATOIRE", java.util.Locale.FRANCE));

		assertThatThrownBy(() -> this.crm.rechercher("??"))
				.isInstanceOf(CritereRechercheManquantException.class);
		this.serveur.verify();
	}

	@Test
	@DisplayName("une panne réseau devient une indisponibilité, jamais une trace")
	void panneReseau_devientIndisponibilite() {
		this.serveur.expect(payload(new StringSource(requeteGetClient("CLI-0001"))))
				.andRespond(withException(new java.io.IOException("connexion refusée")));

		assertThatThrownBy(() -> this.crm.parReference("CLI-0001"))
				.isInstanceOf(CrmIndisponibleException.class);
	}

	@Test
	@DisplayName("un motif vide est refusé sans appeler le CRM")
	void motifVide_neCoutePasUnAppel() {
		// 400 ms économisées pour apprendre ce qu'on savait déjà. Le serveur simulé
		// n'attend aucune requête : s'il en recevait une, verify() le dirait.
		assertThatThrownBy(() -> this.crm.rechercher("   "))
				.isInstanceOf(CritereRechercheManquantException.class);
		this.serveur.verify();
	}

	/**
	 * Le N+1 par le réseau.
	 *
	 * <p>Vingt-cinq tickets touchant trois comptes ne doivent produire que trois appels.
	 * Le cache absorbe les répétitions ; ce test vérifie qu'il fait son travail, et il
	 * échouera si quelqu'un le retire ou si la boucle repasse par référence plutôt que par
	 * référence distincte.
	 */
	@Test
	@DisplayName("un lot de références ne coûte qu'un appel par référence distincte")
	void lot_unAppelParReferenceDistincte() {
		for (String reference : List.of("CLI-0001", "CLI-0002", "CLI-0003")) {
			this.serveur.expect(payload(new StringSource(requeteGetClient(reference))))
					.andRespond(withPayload(new StringSource(reponseGetClient(reference, "Société " + reference))));
		}

		// LinkedHashSet, pas Set.of : le serveur simulé compare les requêtes DANS L'ORDRE,
		// et Set.of n'en garantit aucun. Le test échouait un jour sur trois.
		Set<String> references = new java.util.LinkedHashSet<>(
				List.of("CLI-0001", "CLI-0002", "CLI-0003"));

		// Trois références distinctes demandées deux fois : trois appels attendus, pas six.
		this.repertoire.parReferences(references);
		var deuxieme = this.repertoire.parReferences(references);

		assertThat(deuxieme).hasSize(3);
		// verify() échoue si le serveur simulé a reçu plus de requêtes que prévu.
		this.serveur.verify();
	}

	@Test
	@DisplayName("une référence absente du CRM ne fait pas échouer tout le lot")
	void lot_referenceAbsente_neFaitPasEchouerLeLot() {
		this.serveur.expect(payload(new StringSource(requeteGetClient("CLI-0001"))))
				.andRespond(withPayload(new StringSource(reponseGetClient("CLI-0001", "Transports Nord"))));
		this.serveur.expect(payload(new StringSource(requeteGetClient("CLI-9999"))))
				.andRespond(withClientOrSenderFault("CLIENT_INCONNU", java.util.Locale.FRANCE));

		// Un ticket dont le compte a disparu du CRM doit rester affichable.
		var fiches = this.repertoire.parReferences(new java.util.LinkedHashSet<>(
				List.of("CLI-0001", "CLI-9999")));

		assertThat(fiches).containsOnlyKeys("CLI-0001");
	}

	private static String requeteGetClient(String reference) {
		return "<GetClientRequest xmlns=\"" + NS + "\"><clientRef>" + reference
				+ "</clientRef></GetClientRequest>";
	}

	private static String requeteSearch(String motif) {
		return "<SearchClientsRequest xmlns=\"" + NS + "\"><namePattern>" + motif
				+ "</namePattern></SearchClientsRequest>";
	}

	private static String reponseGetClient(String reference, String raisonSociale) {
		return "<GetClientResponse xmlns=\"" + NS + "\"><client>"
				+ "<clientRef>" + reference + "</clientRef>"
				+ "<raisonSociale>" + raisonSociale + "</raisonSociale>"
				+ "<siret>00000000000000</siret>"
				+ "<contactEmail>contact@exemple.fr</contactEmail>"
				+ "<contactTel>+33 1 00 00 00 00</contactTel>"
				+ "<actif>true</actif></client></GetClientResponse>";
	}
}
