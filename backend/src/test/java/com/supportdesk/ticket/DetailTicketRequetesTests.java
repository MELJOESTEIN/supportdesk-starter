package com.supportdesk.ticket;

import java.util.Set;

import com.supportdesk.BaseIntegration;
import com.supportdesk.securite.UtilisateurCourant;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compte des requêtes SQL d'un appel au détail.
 *
 * <p>C'est le test qui empêche le N+1 de revenir. Il n'affirme pas que le code est
 * « optimisé » : il fixe un plafond, et il tombe dès que le nombre de requêtes se met à
 * dépendre du nombre de commentaires.
 *
 * <p>Regarder les logs suffit pour le constater une fois ; ce test l'empêche pour toujours.
 */
class DetailTicketRequetesTests extends BaseIntegration {

	/** Un agent : il voit tout, ce qui donne le pire cas en nombre de requêtes. */
	private static final UtilisateurCourant AGENT =
			new UtilisateurCourant("bob", Set.of("AGENT"), null);

	@Autowired
	private TicketService service;

	@Autowired
	private TicketRepository tickets;

	@Autowired
	private EntityManagerFactory emf;

	private Statistics statistiques;

	@BeforeEach
	void reinitialiser() {
		this.statistiques = this.emf.unwrap(SessionFactory.class).getStatistics();
		this.statistiques.clear();
	}

	@Test
	@DisplayName("le détail d'un ticket tient en un nombre de requêtes borné")
	void detail_nombreDeRequetesBorne() {
		Long id = idDe("TCK-4821");

		// Remise à zéro APRÈS la résolution de l'identifiant : sinon la requête du test
		// lui-même est comptée dans la mesure, et on cherche un cinquième coupable qui
		// n'existe pas.
		this.statistiques.clear();
		this.service.detail(id, AGENT);

		long requetes = this.statistiques.getPrepareStatementCount();

		// Quatre requêtes : le ticket avec son agent, les commentaires filtrés par
		// visibilité, le journal, et la table des agents pour les noms d'affichage.
		// La première version en faisait six, dont deux redondantes sur `agent`.
		assertThat(requetes)
				.as("détail complet d'un ticket (ticket, commentaires, journal, agents)")
				.isEqualTo(4);
	}

	/**
	 * Le test décisif : le nombre de requêtes ne doit pas dépendre du nombre de
	 * commentaires. Un ticket à cinq commentaires et un ticket à deux doivent coûter le
	 * même nombre d'allers-retours.
	 */
	@Test
	@DisplayName("le nombre de requêtes ne dépend pas du nombre de commentaires")
	void detail_neCroitPasAvecLesCommentaires() {
		Long beaucoup = idDe("TCK-4821");   // 5 commentaires
		Long peu = idDe("TCK-4519");        // 2 commentaires

		this.statistiques.clear();
		this.service.detail(beaucoup, AGENT);
		long avecBeaucoup = this.statistiques.getPrepareStatementCount();

		this.statistiques.clear();
		this.service.detail(peu, AGENT);
		long avecPeu = this.statistiques.getPrepareStatementCount();

		assertThat(avecBeaucoup)
				.as("un N+1 ferait croître ce nombre avec le nombre de commentaires")
				.isEqualTo(avecPeu);
	}

	@Test
	@DisplayName("la liste paginée tient en deux requêtes, quelle que soit la taille de page")
	void liste_deuxRequetesQuelleQueSoitLaPage() {
		this.statistiques.clear();
		this.service.lister(new FiltreTickets(null, null, null, null), AGENT,
				org.springframework.data.domain.PageRequest.of(0, 20));
		long pour20 = this.statistiques.getPrepareStatementCount();

		this.statistiques.clear();
		this.service.lister(new FiltreTickets(null, null, null, null), AGENT,
				org.springframework.data.domain.PageRequest.of(0, 5));
		long pour5 = this.statistiques.getPrepareStatementCount();

		// Une requête de contenu, une de comptage. Ni plus, ni proportionnel à la page.
		assertThat(pour20).isEqualTo(pour5).isLessThanOrEqualTo(2);
	}

	private Long idDe(String reference) {
		return this.tickets.findByReference(reference).orElseThrow().getId();
	}
}
