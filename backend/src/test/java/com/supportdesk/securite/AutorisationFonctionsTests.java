package com.supportdesk.securite;

import com.supportdesk.BaseIntegration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Autorisation au niveau fonction — OWASP API2.
 *
 * <p>Les écrans agent sont masqués côté Angular par une garde de route. Ces tests vérifient
 * ce qui compte : que l'API refuse, garde ou pas.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AutorisationFonctionsTests extends BaseIntegration {

	@Autowired
	private MockMvcTester mvc;

	@Test
	@DisplayName("sans jeton, l'API répond 401")
	void sansJeton_401() {
		assertThat(this.mvc.get().uri("/api/tickets")).hasStatus(401);
	}

	@Test
	@DisplayName("le tableau de bord est refusé à un client")
	void tableauDeBord_refuseAUnClient() {
		assertThat(this.mvc.get().uri("/api/tableau-de-bord").with(Jetons.alice())).hasStatus(403);
		assertThat(this.mvc.get().uri("/api/tableau-de-bord").with(Jetons.bob())).hasStatusOk();
	}

	@Test
	@DisplayName("la liste des agents est refusée à un client")
	void agents_refuseAUnClient() {
		assertThat(this.mvc.get().uri("/api/agents").with(Jetons.alice())).hasStatus(403);
		assertThat(this.mvc.get().uri("/api/agents").with(Jetons.bob())).hasStatusOk();
	}

	@Test
	@DisplayName("la liste des comptes clients est refusée à un client")
	void clientsDeLaFile_refuseAUnClient() {
		// Le CRM refuse d'être énuméré (ClientController). Cet endpoint sert le filtre du
		// back-office : ouvert à un CLIENT, il rendrait par la porte de derrière ce que le
		// CRM protège par la porte de devant — la liste des comptes de la plateforme.
		assertThat(this.mvc.get().uri("/api/tickets/clients").with(Jetons.alice())).hasStatus(403);
		assertThat(this.mvc.get().uri("/api/tickets/clients").with(Jetons.bob())).hasStatusOk();
	}

	@Test
	@DisplayName("un client ne peut pas changer le statut d'un ticket, même le sien")
	void changementDeStatut_refuseAUnClient() {
		// L'interface cliente n'offre aucun bouton de clôture — et ce test vérifie que cette
		// absence n'est PAS la seule protection : l'API refuse, bouton ou pas.
		assertThat(this.mvc.patch().uri("/api/tickets/1").with(Jetons.alice())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"statut\":\"RESOLU\"}"))
				.hasStatus(403);
	}

	@Test
	@DisplayName("Actuator n'expose que health, sans authentification")
	void actuator_exposeSeulementHealth() {
		assertThat(this.mvc.get().uri("/actuator/health")).hasStatusOk();
		assertThat(this.mvc.get().uri("/actuator/env")).hasStatus(404);
	}
}
