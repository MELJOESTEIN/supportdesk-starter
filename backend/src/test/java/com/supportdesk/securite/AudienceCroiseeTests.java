package com.supportdesk.securite;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Un jeton émis pour une AUTRE application du même realm n'ouvre pas cette API.
 *
 * <p>Depuis l'ajout de l'intranet (port 4300), le realm compte deux applications de
 * connexion. Elles partagent la session Keycloak — c'est le but. Elles ne partagent pas les
 * autorisations, et c'est ce que ces tests verrouillent.
 *
 * <h2>Pourquoi ces tests ne passent pas par MockMvc</h2>
 *
 * <p>Parce que ce serait un test creux. {@code SecurityMockMvcRequestPostProcessors.jwt()},
 * qu'utilise {@link Jetons}, <b>court-circuite le {@code JwtDecoder}</b> : il injecte une
 * authentification déjà construite. Aucune requête MockMvc du projet ne traverse
 * {@link ValidateurAudience}. Un test HTTP avec une mauvaise audience renverrait 200 et ne
 * prouverait rien — pire, il donnerait l'illusion d'une protection vérifiée.
 *
 * <p>Le seul chemin qui exercerait vraiment le décodeur demande un jeton réellement signé et
 * un JWKS joignable, donc un Keycloak debout : ce n'est plus un test, c'est
 * {@code verif/60-sso.http}, qui existe pour ça.
 *
 * <p>Ce qui reste testable ici, et qui manquait, c'est le <b>câblage</b> : la chaîne exercée
 * est celle que {@link ConfigurationSecurite#decodeurJwt()} installe réellement, et non un
 * {@code new ValidateurAudience(...)} reconstruit pour l'occasion.
 */
class AudienceCroiseeTests {

	private static final String ISSUER = "http://localhost:8081/realms/supportdesk";

	private final OAuth2TokenValidator<Jwt> chaine = ConfigurationSecurite.validateurs(ISSUER,
			"supportdesk-api");

	@Test
	@DisplayName("un jeton emis pour l'intranet est refuse par l'API SupportDesk")
	void jetonIntranet_refuse() {
		assertThat(this.chaine.validate(jeton(List.of("intranet-api"))).hasErrors())
				.as("un jeton de l'intranet ne doit pas ouvrir l'API SupportDesk")
				.isTrue();
	}

	@Test
	@DisplayName("un jeton emis pour SupportDesk est accepte")
	void jetonSupportdesk_accepte() {
		// Le contre-test, sans lequel le précédent ne vaut rien : une chaîne qui refuse TOUT
		// ferait passer le premier test avec les honneurs.
		assertThat(this.chaine.validate(jeton(List.of("supportdesk-api"))).hasErrors())
				.as("le jeton légitime doit passer")
				.isFalse();
	}

	@Test
	@DisplayName("un jeton portant les deux audiences est accepte")
	void jetonMultiAudience_accepte() {
		// Cas réel : un mappeur mal réglé ajoute les deux audiences. La règle est
		// « contient », pas « égale » — un jeton légitime pour SupportDesk le reste même s'il
		// vaut aussi pour l'intranet. C'est la sémantique de `aud` dans la RFC 7519.
		assertThat(this.chaine.validate(jeton(List.of("intranet-api", "supportdesk-api"))).hasErrors())
				.isFalse();
	}

	@Test
	@DisplayName("un jeton sans audience du tout est refuse")
	void jetonSansAudience_refuse() {
		assertThat(this.chaine.validate(jeton(List.of())).hasErrors()).isTrue();
	}

	private static Jwt jeton(List<String> audience) {
		Instant maintenant = Instant.now();
		return Jwt.withTokenValue("jeton-de-test")
				.header("alg", "RS256")
				.claim("iss", ISSUER)
				.claim("aud", audience)
				.claim("preferred_username", "alice")
				.claim("realm_access", Map.of("roles", List.of("CLIENT")))
				.issuedAt(maintenant.minus(1, ChronoUnit.MINUTES))
				.expiresAt(maintenant.plus(5, ChronoUnit.MINUTES))
				.build();
	}

}
