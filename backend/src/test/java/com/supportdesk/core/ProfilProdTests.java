package com.supportdesk.core;

import com.supportdesk.BaseIntegration;
import com.supportdesk.securite.Jetons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ce que la production n'expose pas.
 *
 * <p>Sans ce test, la coupure de Swagger repose sur un fichier YAML que personne ne relit.
 * Ici, la remettre en service casse le build.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "test", "prod" })
class ProfilProdTests extends BaseIntegration {

	@Autowired
	private MockMvcTester mvc;

	@Test
	@DisplayName("Swagger UI n'existe pas en production")
	void swagger_absentEnProd() {
		assertThat(this.mvc.get().uri("/swagger-ui.html")).hasStatus(404);
	}

	@Test
	@DisplayName("la description OpenAPI n'est pas publiée en production")
	void apiDocs_absentEnProd() {
		assertThat(this.mvc.get().uri("/v3/api-docs")).hasStatus(404);
	}

	@Test
	@DisplayName("Actuator n'expose que health en production")
	void actuator_exposeSeulementHealth() {
		assertThat(this.mvc.get().uri("/actuator/health")).hasStatusOk();

		// env et beans révèlent la configuration complète, jusqu'aux URL de base et aux
		// noms d'utilisateur. Ils n'ont rien à faire sur un port public.
		assertThat(this.mvc.get().uri("/actuator/env")).hasStatus(404);
		assertThat(this.mvc.get().uri("/actuator/beans")).hasStatus(404);
	}

	@Test
	@DisplayName("l'API reste servie normalement en production")
	void api_repondEnProd() {
		// Contre-test : sans lui, une application cassée ferait passer les trois précédents.
		assertThat(this.mvc.get().uri("/api/tickets?taille=1").with(Jetons.alice())).hasStatusOk();
	}
}
