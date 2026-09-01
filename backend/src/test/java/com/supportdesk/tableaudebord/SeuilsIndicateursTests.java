package com.supportdesk.tableaudebord;

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
 * Les seuils affichés par le tableau de bord doivent être comparés, pas décorés.
 *
 * <p>La recette du 30 août : « SLA RESPECTÉ 4,3 % » face à « seuil contractuel 90 % » portait
 * le liseré gris d'un compteur ordinaire, tandis que « sans réponse &gt; 48 h » virait au
 * rouge pour un effectif de deux. Le chiffre le plus grave du tableau était le moins visible.
 *
 * <p>La cause tenait en un mot : {@code alerte} valait {@code false} en dur pour ces deux
 * indicateurs. L'objectif écrit sous la valeur n'était pas un seuil, c'était une légende.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeuilsIndicateursTests extends BaseIntegration {

	@Autowired
	private MockMvcTester mvc;

	@Test
	@DisplayName("un SLA sous le seuil contractuel declenche l'alerte")
	void slaSousLeSeuil_alerte() {
		var corps = assertThat(this.mvc.get().uri("/api/tableau-de-bord").with(Jetons.bob()))
				.hasStatusOk().bodyJson();

		// Le jeu de démonstration est très en dessous de 90 % — c'est voulu, la file sert à
		// montrer un service en difficulté.
		corps.extractingPath("$.indicateurs[?(@.cle == 'SLA_RESPECTE')].alerte").asArray()
				.containsExactly(true);
	}

	@Test
	@DisplayName("une premiere reponse mediane au-dela de l'objectif declenche l'alerte")
	void premiereReponseTropLente_alerte() {
		var corps = assertThat(this.mvc.get().uri("/api/tableau-de-bord").with(Jetons.bob()))
				.hasStatusOk().bodyJson();

		corps.extractingPath("$.indicateurs[?(@.cle == 'PREMIERE_REPONSE_MEDIANE')].alerte").asArray()
				.containsExactly(true);
	}

	@Test
	@DisplayName("un indicateur sans objectif ne declenche jamais d'alerte")
	void indicateurSansObjectif_jamaisEnAlerte() {
		var corps = assertThat(this.mvc.get().uri("/api/tableau-de-bord").with(Jetons.bob()))
				.hasStatusOk().bodyJson();

		// Un compteur n'a pas de seuil : « tickets ouverts » n'est ni bon ni mauvais. Sans
		// ce test, la tentation serait de tout mettre en alerte, et plus rien ne ressortirait.
		corps.extractingPath("$.indicateurs[?(@.cle == 'OUVERTS')].alerte").asArray()
				.containsExactly(false);
	}

}
