package com.supportdesk.ticket;

import com.supportdesk.BaseIntegration;
import com.supportdesk.securite.Jetons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ordre du journal d'un ticket.
 *
 * <p>La recette du 30 août lisait le journal de TCK-4821 dans cet ordre : note interne,
 * assignation, <b>création du ticket</b>, note interne. Une création qui n'est pas le
 * dernier événement d'une liste antichronologique.
 *
 * <p>La cause n'était pas le jeu de données. {@code TicketService#modifier} journalise le
 * changement de statut, celui de priorité et l'assignation avec le <b>même</b>
 * {@code Instant} : les égalités sont garanties en usage normal, et le tri n'avait aucun
 * second critère. L'ordre revenait alors à Postgres, qui le choisit selon son plan
 * d'exécution — un journal qui peut se réordonner entre deux consultations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JournalTicketTests extends BaseIntegration {

	@Autowired
	private MockMvcTester mvc;

	@Test
	@DisplayName("trois evenements ecrits au meme instant sortent dans un ordre stable")
	void memeInstant_ordreStable() {
		// Un seul PATCH, trois journalisations, un seul appel à l'horloge : c'est le cas
		// courant du back-office, pas un cas limite fabriqué pour le test.
		assertThat(this.mvc.patch().uri("/api/tickets/1").with(Jetons.bob())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"statut":"EN_COURS","priorite":"HAUTE","assigneA":"carol"}
						""")).hasStatusOk();

		var corps = assertThat(this.mvc.get().uri("/api/tickets/1").with(Jetons.bob()))
				.hasStatusOk().bodyJson();

		// Écrits dans l'ordre statut, priorité, assignation ; rendus du plus récent au plus
		// ancien, donc exactement à l'envers. Sans départage par identifiant, l'ordre
		// obtenu est celui que Postgres juge commode — en pratique celui de l'insertion,
		// c'est-à-dire l'inverse de celui-ci.
		corps.extractingPath("$.evenements[*].type").asArray()
				.startsWith("ASSIGNATION", "CHANGEMENT_PRIORITE", "CHANGEMENT_STATUT");
	}

	@Test
	@DisplayName("la creation ferme toujours la marche du journal")
	void creation_enDernier() {
		var corps = assertThat(this.mvc.get().uri("/api/tickets/1").with(Jetons.bob()))
				.hasStatusOk().bodyJson();

		// Rien ne peut précéder la création du ticket. Si cette assertion tombe, c'est soit
		// un horodatage impossible dans le jeu de données, soit un tri sans départage.
		corps.extractingPath("$.evenements[*].type").asArray().endsWith("CREATION");
	}

}
