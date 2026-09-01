package com.supportdesk.ticket;

import com.supportdesk.BaseIntegration;
import com.supportdesk.securite.Jetons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Une note interne ne doit jamais atteindre un client.
 *
 * <p>Ces tests portent sur le <b>corps de la réponse HTTP</b>, pas sur l'affichage. Une
 * interface qui masque une note interne déjà transmise ne protège rien : la donnée est dans
 * le navigateur, dans les journaux du proxy, dans le cache.
 *
 * <p>En Boot 4, {@code @SpringBootTest} ne fournit plus MockMvc automatiquement :
 * {@code @AutoConfigureMockMvc} est explicite.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ConfidentialiteNotesInternesTests extends BaseIntegration {

	private static final String EXTRAIT_NOTE_INTERNE = "Ne pas mentionner la migration au client";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private TicketRepository tickets;

	@Test
	@DisplayName("la réponse servie à un client ne contient pas le texte d'une note interne")
	void vueClient_neTransporteAucuneNoteInterne() throws Exception {
		Long id = idDe("TCK-4821");

		String corps = this.mvc.get().uri("/api/tickets/{id}", id).with(Jetons.alice()).exchange().getResponse()
				.getContentAsString();

		assertThat(corps)
				.as("le corps HTTP servi à un client ne doit contenir aucune note interne")
				.doesNotContain(EXTRAIT_NOTE_INTERNE)
				.doesNotContain("INTERNE");
	}

	@Test
	@DisplayName("la même note est bien présente dans la vue agent — sinon le test ne prouverait rien")
	void vueAgent_transporteBienLaNoteInterne() throws Exception {
		Long id = idDe("TCK-4821");

		String corps = this.mvc.get().uri("/api/tickets/{id}", id).with(Jetons.bob()).exchange()
				.getResponse().getContentAsString();

		// Ce contre-test est indispensable : sans lui, un endpoint cassé qui ne renvoie
		// jamais de commentaires ferait passer le test précédent.
		assertThat(corps).contains(EXTRAIT_NOTE_INTERNE);
	}

	@Test
	@DisplayName("le compteur de messages d'un client n'inclut pas les notes internes")
	void vueClient_compteurNInclutPasLesNotesInternes() {
		Long id = idDe("TCK-4821");

		assertThat(this.mvc.get().uri("/api/tickets/{id}", id).with(Jetons.alice())).hasStatusOk()
				.bodyJson().extractingPath("$.nombreMessages").isEqualTo(3);
	}

	@Test
	@DisplayName("le journal d'un ticket n'est jamais servi à un client")
	void vueClient_neRecoitPasLeJournal() {
		Long id = idDe("TCK-4821");

		assertThat(this.mvc.get().uri("/api/tickets/{id}", id).with(Jetons.alice())).hasStatusOk()
				.bodyJson().extractingPath("$.evenements").asArray().isEmpty();
	}

	private Long idDe(String reference) {
		return this.tickets.findByReference(reference).orElseThrow().getId();
	}
}
