package com.supportdesk.graphql;

import com.supportdesk.BaseIntegration;
import com.supportdesk.securite.Jetons;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API GraphQL, éprouvée à travers la vraie chaîne HTTP.
 *
 * <p>Volontairement pas un {@code @GraphQlTest} : cette tranche court-circuite la couche web,
 * donc la sécurité. Or la moitié de ce qu'on veut vérifier ici <b>est</b> la sécurité — un
 * client refusé, une mutation réservée aux agents. Un test qui contourne le filtre ne prouve
 * rien sur ce point.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TicketGraphQlTests extends BaseIntegration {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private EntityManagerFactory emf;

	private Statistics statistiques;

	@BeforeEach
	void reinitialiser() {
		this.statistiques = this.emf.unwrap(SessionFactory.class).getStatistics();
		this.statistiques.clear();
	}

	@Test
	@DisplayName("un agent interroge la file avec ses clients et ses assignations")
	void agent_interrogeLaFile() {
		var corps = executer(Jetons.bob(),
				"{ tickets(taille: 3) { total contenu { reference statut assigneA { nomComplet } } } }")
				.hasStatusOk().bodyJson();

		// Pas d'assertion sur $.errors : le chemin est absent quand tout va bien, et
		// `extractingPath` échoue sur un chemin absent au lieu de le traiter comme null.
		corps.extractingPath("$.data.tickets.contenu").asArray().hasSize(3);
		// Pas d'assertion sur un ticket précis : l'ordre dépend de la dernière activité, et
		// un test voisin qui crée un ticket changerait la tête de liste.
		corps.extractingPath("$.data.tickets.contenu[*].reference").asArray().hasSize(3);
	}

	@Test
	@DisplayName("un client est refusé sur /graphql")
	void client_estRefuse() {
		// La chaîne de filtres réserve /graphql aux agents. Sans elle, il resterait
		// @PreAuthorize sur chaque méthode — deux barrières, et c'est voulu.
		executer(Jetons.alice(), "{ tickets { total } }").hasStatus(403);
	}

	@Test
	@DisplayName("sans jeton, /graphql répond 401")
	void sansJeton_401() {
		assertThat(this.mvc.post().uri("/graphql").contentType(MediaType.APPLICATION_JSON)
				.content("{\"query\":\"{ tickets { total } }\"}")).hasStatus(401);
	}

	/**
	 * Le N+1, version GraphQL.
	 *
	 * <p>Sans {@code @BatchMapping}, le champ {@code assigneA} serait résolu ticket par
	 * ticket : une requête SQL par ligne. Le test compare deux tailles de page — si le
	 * nombre de requêtes suivait la taille, le batching ne se ferait pas.
	 */
	@Test
	@DisplayName("le nombre de requêtes SQL ne suit pas la taille de la page")
	void batchMapping_neFaitPasDeNPlusUn() {
		this.statistiques.clear();
		executer(Jetons.bob(), "{ tickets(taille: 5) { contenu { reference assigneA { nomComplet } } } }")
				.hasStatusOk();
		long pour5 = this.statistiques.getPrepareStatementCount();

		this.statistiques.clear();
		executer(Jetons.bob(), "{ tickets(taille: 20) { contenu { reference assigneA { nomComplet } } } }")
				.hasStatusOk();
		long pour20 = this.statistiques.getPrepareStatementCount();

		assertThat(pour20)
				.as("5 tickets : %d requêtes ; 20 tickets : %d — un N+1 les ferait diverger",
						pour5, pour20)
				.isEqualTo(pour5);
	}

	@Test
	@DisplayName("l'amplification par alias est arrêtée par la borne de complexité")
	void amplificationParAlias_estArretee() {
		StringBuilder requete = new StringBuilder("{ ");
		for (int i = 0; i < 30; i++) {
			requete.append("a").append(i)
					.append(": tickets(taille: 100) { contenu { reference sujet statut priorite ")
					.append("assigneA { nomComplet equipe niveau } } } ");
		}
		requete.append("}");

		var corps = executer(Jetons.bob(), requete.toString()).bodyJson();

		// La profondeur seule ne verrait rien : trente alias restent plats.
		corps.extractingPath("$.errors[0].message").asString().contains("complexity");
	}

	@Test
	@DisplayName("un ticket inexistant ne fait pas tomber la requête entière")
	void ticketInexistant_erreurDeChamp() {
		var corps = executer(Jetons.bob(), "{ ticket(id: 999999) { reference } }").hasStatusOk()
				.bodyJson();

		// Erreur de champ, HTTP 200, chemin renseigné : c'est le protocole GraphQL, et
		// c'est ce qui permet à un écran de rendre partiellement.
		corps.extractingPath("$.errors[0].path[0]").isEqualTo("ticket");
	}

	private org.springframework.test.web.servlet.assertj.MvcTestResultAssert executer(
			org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
					.JwtRequestPostProcessor jeton,
			String requete) {
		String corps = "{\"query\":" + jsonEchappe(requete) + "}";
		return assertThat(this.mvc.post().uri("/graphql").with(jeton)
				.contentType(MediaType.APPLICATION_JSON).content(corps));
	}

	private static String jsonEchappe(String valeur) {
		return "\"" + valeur.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\"";
	}
}
