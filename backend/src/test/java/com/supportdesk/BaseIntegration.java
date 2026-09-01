package com.supportdesk;

import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;

/**
 * Socle des tests d'intégration : un vrai PostgreSQL 18, la même version qu'en production.
 *
 * <p>Pas de H2. Une base en mémoire ne connaît ni {@code TIMESTAMPTZ}, ni
 * {@code generate_series}, ni les contraintes {@code CHECK} telles que Postgres les applique :
 * un test qui passe sur H2 ne prouve rien sur ce projet.
 *
 * <p><b>Conteneur singleton, démarré dans un bloc statique</b> — et non {@code @Container} +
 * {@code @Testcontainers}. Avec l'extension JUnit, le conteneur déclaré dans une classe
 * parente est arrêté à la fin de la première classe de test qui en hérite ; les suivantes
 * échouent sur « Failed to obtain JDBC Connection », trente secondes plus tard, sans que la
 * cause apparaisse nulle part. Ici, le conteneur vit le temps de la JVM et Ryuk le nettoie.
 *
 * <p>Testcontainers 2.x a déplacé les modules : {@code org.testcontainers.postgresql},
 * artefact {@code testcontainers-postgresql}. Les conteneurs ne sont plus génériques :
 * {@code PostgreSQLContainer}, sans {@code <?>}.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegration {

	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

	static {
		POSTGRES.start();
	}
}
