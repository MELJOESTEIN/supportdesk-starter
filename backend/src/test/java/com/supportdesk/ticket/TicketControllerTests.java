package com.supportdesk.ticket;

import java.time.Instant;
import java.util.List;

import com.supportdesk.agent.AgentResume;
import com.supportdesk.core.PageReponse;
import com.supportdesk.ticket.TicketDtos.TicketDetail;
import com.supportdesk.ticket.TicketDtos.TicketResume;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

/**
 * Tranche web du contrôleur des tickets.
 *
 * <p>En Boot 4, {@code MockMvcTester} remplace le style fluide de {@code MockMvc} et
 * {@code @MockitoBean} remplace {@code @MockBean}.
 */
@WebMvcTest(TicketController.class)
class TicketControllerTests {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private TicketService service;

	@Test
	@DisplayName("la page renvoyée porte exactement les champs du contrat TypeScript")
	void liste_respecteLeContrat() {
		given(this.service.lister(any(), anyBoolean(), any())).willReturn(unePage());

		var corps = assertThat(this.mvc.get().uri("/api/tickets")).hasStatusOk().bodyJson();

		corps.extractingPath("$.page").isEqualTo(0);
		corps.extractingPath("$.taille").isEqualTo(20);
		corps.extractingPath("$.total").isEqualTo(1);
		corps.extractingPath("$.totalPages").isEqualTo(1);
		corps.extractingPath("$.contenu[0].reference").isEqualTo("TCK-4821");
		corps.extractingPath("$.contenu[0].crmClientRef").isEqualTo("CLI-0001");
		corps.extractingPath("$.contenu[0].assigneA.nomComplet").isEqualTo("Bob Lefevre");
		corps.extractingPath("$.contenu[0].clientRaisonSociale").isNull();

		// Jackson 3 écrit les dates en ISO-8601, plus en epoch : un client qui attendait un
		// nombre casserait ici.
		corps.extractingPath("$.contenu[0].creeLe").asString().startsWith("2026-08-24T");
	}

	@Test
	@DisplayName("un ticket inexistant renvoie un ProblemDetail 404, pas une trace")
	void detail_inexistant_renvoieProblemDetail() {
		willThrow(new TicketIntrouvableException(999L)).given(this.service).detail(eq(999L), anyBoolean());

		var corps = assertThat(this.mvc.get().uri("/api/tickets/999"))
				.hasStatus(404)
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson();

		// Le `type` est une URI stable : c'est sur elle qu'un client branche sa logique.
		corps.extractingPath("$.type")
				.isEqualTo("https://supportdesk.local/erreurs/ticket-introuvable");
		corps.extractingPath("$.ticketId").isEqualTo(999);
	}

	@Test
	@DisplayName("un sujet vide est refusé avec le détail par champ")
	void creation_sujetVide_renvoieProblemDetail() {
		assertThat(this.mvc.post().uri("/api/tickets?auteurUsername=alice&crmClientRef=CLI-0001")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"sujet": "  ", "categorie": "FACTURATION", "description": "Bonjour"}
						"""))
				.hasStatus(400)
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson()
				.extractingPath("$.champs.sujet").isEqualTo("Le sujet est obligatoire");
	}

	@Test
	@DisplayName("un sujet de plus de 80 caractères est refusé")
	void creation_sujetTropLong_estRefuse() {
		String sujet = "x".repeat(81);

		assertThat(this.mvc.post().uri("/api/tickets?auteurUsername=alice&crmClientRef=CLI-0001")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"sujet\": \"" + sujet + "\", \"categorie\": \"AUTRE\", \"description\": \"d\"}"))
				.hasStatus(400);
	}

	/**
	 * Affectation en masse : le DTO d'entrée ne doit pas offrir de prise sur le statut.
	 *
	 * <p>Jackson ignore les champs inconnus par défaut, donc l'envoi ne casse pas — il ne
	 * change simplement rien. Ce test fige ce comportement : si quelqu'un ajoute un jour
	 * {@code statut} au DTO, le service recevra la valeur du client et le test le dira.
	 */
	@Test
	@DisplayName("un statut envoyé par le client est ignoré")
	void creation_statutImpose_estIgnore() {
		given(this.service.creer(any(), any(), any())).willReturn(unDetail());

		assertThat(this.mvc.post().uri("/api/tickets?auteurUsername=alice&crmClientRef=CLI-0001")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"sujet": "Test", "categorie": "AUTRE", "description": "d", "statut": "RESOLU",
						 "crmClientRef": "CLI-0002", "assigneA": "bob"}
						"""))
				.hasStatus(201)
				.bodyJson()
				.extractingPath("$.statut").isEqualTo("OUVERT");
	}

	private static PageReponse<TicketResume> unePage() {
		return new PageReponse<>(List.of(new TicketResume(1L, "TCK-4821", "Facture de mars",
				StatutTicket.OUVERT, PrioriteTicket.NORMALE, "CLI-0001", null,
				new AgentResume("bob", "Bob Lefevre", "Niveau 2", "Facturation"),
				Instant.parse("2026-08-24T09:12:00Z"), Instant.parse("2026-08-29T08:26:00Z"),
				"Bob Lefevre", 3, false)), 0, 20, 1, 1);
	}

	private static TicketDetail unDetail() {
		return new TicketDetail(1L, "TCK-4900", "Test", StatutTicket.OUVERT, PrioriteTicket.NORMALE,
				"CLI-0001", null, null, Instant.parse("2026-08-30T08:00:00Z"),
				Instant.parse("2026-08-30T08:00:00Z"), "alice", 1, false, "d", CategorieTicket.AUTRE,
				List.of(), List.of());
	}
}
