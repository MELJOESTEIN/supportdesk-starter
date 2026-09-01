package com.supportdesk.ticket;

import com.supportdesk.BaseIntegration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le schéma est celui de Flyway, et Hibernate le valide au démarrage
 * ({@code ddl-auto: validate}). Que ces tests démarrent prouve déjà les deux.
 */
class MigrationsTests extends BaseIntegration {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	@DisplayName("les deux migrations sont appliquées")
	void migrations_appliquees() {
		Integer appliquees = this.jdbc.queryForObject(
				"SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE", Integer.class);

		assertThat(appliquees).isEqualTo(2);
	}

	@Test
	@DisplayName("le jeu de données couvre les deux comptes de la démonstration BOLA")
	void seed_couvreLesDeuxComptes() {
		Integer cli1 = this.jdbc.queryForObject(
				"SELECT COUNT(*) FROM ticket WHERE crm_client_ref = 'CLI-0001'", Integer.class);
		Integer cli2 = this.jdbc.queryForObject(
				"SELECT COUNT(*) FROM ticket WHERE crm_client_ref = 'CLI-0002'", Integer.class);

		assertThat(cli1).isPositive();
		assertThat(cli2).isPositive();
	}

	@Test
	@DisplayName("le jeu de données contient des notes internes — sans elles, rien à protéger")
	void seed_contientDesNotesInternes() {
		Integer internes = this.jdbc.queryForObject(
				"SELECT COUNT(*) FROM commentaire WHERE visibilite = 'INTERNE'", Integer.class);

		assertThat(internes).isGreaterThan(5);
	}

	/**
	 * La base ne fait pas confiance à l'application.
	 *
	 * <p>Si un jour un chemin de code contourne l'enum Java — un script, une migration, un
	 * import — la contrainte {@code CHECK} refuse quand même la valeur.
	 */
	@Test
	@DisplayName("un statut hors énumération est refusé par la base elle-même")
	void contrainte_statutInvalide_refuseParLaBase() {
		assertThatThrownBy(() -> this.jdbc.update("""
				INSERT INTO ticket (reference, sujet, description, categorie, statut, priorite,
				    crm_client_ref, auteur_username, cree_le, derniere_activite_le)
				VALUES ('TCK-9999', 's', 'd', 'AUTRE', 'ARCHIVE', 'NORMALE', 'CLI-0001', 'alice',
				        now(), now())
				"""))
				.hasMessageContaining("ck_ticket_statut");
	}

	@Test
	@DisplayName("une visibilité hors énumération est refusée par la base elle-même")
	void contrainte_visibiliteInvalide_refuseParLaBase() {
		assertThatThrownBy(() -> this.jdbc.update("""
				INSERT INTO commentaire (ticket_id, auteur_username, auteur_type, contenu, visibilite, cree_le)
				SELECT id, 'alice', 'CLIENT', 'x', 'SEMI_PRIVE', now() FROM ticket LIMIT 1
				"""))
				.hasMessageContaining("ck_commentaire_visibilite");
	}
}
