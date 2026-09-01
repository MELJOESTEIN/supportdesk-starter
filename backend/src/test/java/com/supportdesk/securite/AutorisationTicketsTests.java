package com.supportdesk.securite;

import com.supportdesk.BaseIntegration;
import com.supportdesk.ticket.TicketRepository;

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
 * Autorisation au niveau objet — OWASP API1 (BOLA).
 *
 * <p>Le test central du projet. Il rejoue, en automatique, la démonstration du J2 : alice
 * (CLI-0001) tente de lire un ticket de david (CLI-0002).
 */
@SpringBootTest
@AutoConfigureMockMvc
// Les tests de création écrivent en base. Sans rollback, ils laissent des tickets derrière
// eux et font échouer d'autres classes — un test doit rendre la base telle qu'il l'a
// trouvée. MockMvc s'exécutant dans le thread du test, la transaction couvre bien l'appel.
@Transactional
class AutorisationTicketsTests extends BaseIntegration {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private TicketRepository tickets;

	@Test
	@DisplayName("un client ne lit pas le ticket d'un autre compte")
	void client_neLitPasLeTicketDunAutreClient() {
		Long ticketDeDavid = idDe("TCK-4818");

		assertThat(this.mvc.get().uri("/api/tickets/{id}", ticketDeDavid).with(Jetons.alice()))
				.hasStatus(403)
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson().extractingPath("$.type")
				.isEqualTo("https://supportdesk.local/erreurs/ticket-autre-compte");
	}

	@Test
	@DisplayName("la réponse 403 ne divulgue aucune donnée du ticket")
	void refus_neDivulgueRien() throws Exception {
		Long ticketDeDavid = idDe("TCK-4818");

		String corps = this.mvc.get().uri("/api/tickets/{id}", ticketDeDavid).with(Jetons.alice())
				.exchange().getResponse().getContentAsString();

		assertThat(corps)
				.doesNotContain("avoir non recu")
				.doesNotContain("CLI-0002")
				.doesNotContain("TCK-4818");
	}

	/** Contre-test : sans lui, un endpoint cassé ferait passer les deux précédents. */
	@Test
	@DisplayName("le même client lit sans problème ses propres tickets")
	void client_litSesPropresTickets() {
		assertThat(this.mvc.get().uri("/api/tickets/{id}", idDe("TCK-4821")).with(Jetons.alice()))
				.hasStatusOk();
	}

	@Test
	@DisplayName("un agent lit le ticket de n'importe quel compte")
	void agent_litNimporteQuelTicket() {
		assertThat(this.mvc.get().uri("/api/tickets/{id}", idDe("TCK-4818")).with(Jetons.bob()))
				.hasStatusOk();
	}

	@Test
	@DisplayName("le paramètre crmClientRef ne peut pas élargir le périmètre")
	void parametreCrmClientRef_estIgnore() {
		var corps = assertThat(this.mvc.get().uri("/api/tickets?crmClientRef=CLI-0002&taille=100")
				.with(Jetons.alice())).hasStatusOk().bodyJson();

		// Une valeur envoyée par le client peut décider de CE QU'IL DEMANDE,
		// jamais de CE QU'IL A LE DROIT D'OBTENIR.
		corps.extractingPath("$.contenu[*].crmClientRef").asArray().containsOnly("CLI-0001");
	}

	/**
	 * Un défaut permissif est la variante silencieuse de la même faille : sans référence,
	 * la tentation est de ne pas filtrer, donc de tout montrer.
	 */
	@Test
	@DisplayName("un client sans référence CRM est refusé, pas élargi")
	void clientSansReference_estRefuse() {
		// Écrit avant la correction, ce test a effectivement échoué : le compte orphelin
		// voyait les 25 tickets de tous les comptes. « Pas de référence, donc pas de
		// filtre » est le réflexe qui produit la faille.
		assertThat(this.mvc.get().uri("/api/tickets?taille=100").with(Jetons.clientSansReference()))
				.hasStatus(403)
				.bodyJson().extractingPath("$.type")
				.isEqualTo("https://supportdesk.local/erreurs/compte-non-rattache");

		assertThat(this.mvc.get().uri("/api/tickets/{id}", idDe("TCK-4821"))
				.with(Jetons.clientSansReference())).hasStatus(403);
	}

	@Test
	@DisplayName("écrire sur le ticket d'un autre compte est refusé comme le lire")
	void client_neCommentePasLeTicketDunAutre() {
		assertThat(this.mvc.post().uri("/api/tickets/{id}/commentaires", idDe("TCK-4818"))
				.with(Jetons.alice())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"contenu\":\"bonjour\",\"visibilite\":\"PUBLIC\"}"))
				.hasStatus(403);
	}

	@Test
	@DisplayName("un client ne peut pas écrire de note interne, même sur son ticket")
	void client_nEcritPasDeNoteInterne() {
		assertThat(this.mvc.post().uri("/api/tickets/{id}/commentaires", idDe("TCK-4821"))
				.with(Jetons.alice())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"contenu\":\"note\",\"visibilite\":\"INTERNE\"}"))
				.hasStatus(403)
				.bodyJson().extractingPath("$.type")
				.isEqualTo("https://supportdesk.local/erreurs/visibilite-interdite");
	}

	@Test
	@DisplayName("un agent ne crée pas de ticket : refus explicite, pas une erreur 500")
	void agent_neCreePasDeTicket() {
		// Trouvé par la revue OWASP : le cas produisait une violation de contrainte NOT NULL,
		// donc un 500. Un refus doit être une décision, pas un accident.
		assertThat(this.mvc.post().uri("/api/tickets").with(Jetons.bob())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"sujet\":\"s\",\"categorie\":\"AUTRE\",\"description\":\"d\"}"))
				.hasStatus(403)
				.bodyJson().extractingPath("$.type")
				.isEqualTo("https://supportdesk.local/erreurs/creation-reservee-au-client");
	}

	@Test
	@DisplayName("le client ne fixe ni statut, ni priorité, ni propriétaire à la création")
	void creation_ignoreLesChampsImposes() {
		var corps = assertThat(this.mvc.post().uri("/api/tickets").with(Jetons.alice())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"sujet":"essai","categorie":"AUTRE","description":"x",
						 "statut":"RESOLU","assigneA":"bob","crmClientRef":"CLI-0002","priorite":"HAUTE"}
						"""))
				.hasStatus(201).bodyJson();

		// Affectation en masse (OWASP API3) : rien de ce que le client a glissé ne passe.
		corps.extractingPath("$.statut").isEqualTo("OUVERT");
		corps.extractingPath("$.priorite").isEqualTo("NORMALE");
		corps.extractingPath("$.assigneA").isNull();
		corps.extractingPath("$.crmClientRef").isEqualTo("CLI-0001");
	}

	@Test
	@DisplayName("un ticket inexistant reste un 404, pas un 403")
	void ticketInexistant_reste404() {
		// Un 403 indifférencié empêcherait de distinguer « n'existe pas » de « pas à vous »,
		// et ferait mentir l'écran qui nomme le compte propriétaire.
		assertThat(this.mvc.get().uri("/api/tickets/999999").with(Jetons.bob())).hasStatus(404);
	}

	private Long idDe(String reference) {
		return this.tickets.findByReference(reference).orElseThrow().getId();
	}
}
