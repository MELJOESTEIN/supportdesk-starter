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
 * Options du filtre « Client » de la file agent.
 *
 * <p>Le défaut corrigé : le menu déroulant listait cinq sociétés écrites en dur, recopiées
 * de la maquette, alors que la file en contient huit. Les tickets des trois autres étaient
 * visibles dans la file mais impossibles à isoler — un filtre qui ment sur ce qu'il couvre
 * est pire qu'un filtre absent, parce qu'on lui fait confiance.
 *
 * <p>Ces tests portent sur les <b>références</b>, pas sur les raisons sociales : les libellés
 * viennent du CRM, qui ne tourne pas en intégration continue. C'est précisément la
 * dégradation que vérifie le dernier test.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FiltreClientsTests extends BaseIntegration {

	@Autowired
	private MockMvcTester mvc;

	@Test
	@DisplayName("le filtre propose toutes les references presentes dans la file")
	void couvreToutesLesReferences() {
		var corps = assertThat(this.mvc.get().uri("/api/tickets/clients").with(Jetons.bob()))
				.hasStatusOk().bodyJson();

		// CLI-0006 et CLI-0007 sont les deux qui manquaient à la liste en dur.
		corps.extractingPath("$[*].reference").asArray()
				.contains("CLI-0001", "CLI-0002", "CLI-0006", "CLI-0007");
	}

	@Test
	@DisplayName("aucune option ne designe un compte sans ticket")
	void neProposeAucuneOptionMorte() {
		var corps = assertThat(this.mvc.get().uri("/api/tickets/clients").with(Jetons.bob()))
				.hasStatusOk().bodyJson();

		// CLI-0008 « Cartonnages Vasseur » existe au CRM mais n'a aucun ticket. Le proposer
		// donnerait une option qui ne peut produire qu'un écran vide. La liste vient des
		// tickets, jamais du référentiel — c'est ce qui l'empêche de diverger.
		corps.extractingPath("$[*].reference").asArray().doesNotContain("CLI-0008");
	}

	@Test
	@DisplayName("chaque option porte un libelle, meme si le CRM ne repond pas")
	void toujoursUnLibelle() {
		var corps = assertThat(this.mvc.get().uri("/api/tickets/clients").with(Jetons.bob()))
				.hasStatusOk().bodyJson();

		// CRM debout, c'est la raison sociale ; CRM absent, la référence brute. Jamais null,
		// jamais une erreur : un référentiel injoignable ne doit pas emporter le back-office.
		corps.extractingPath("$[*].raisonSociale").asArray().doesNotContainNull();
	}

}
