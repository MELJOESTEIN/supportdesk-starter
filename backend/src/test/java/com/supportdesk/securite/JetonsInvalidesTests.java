package com.supportdesk.securite;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.supportdesk.securite.ConvertisseurAutorites;
import com.supportdesk.securite.ValidateurAudience;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ce que le resource server doit refuser, et comment il lit les rôles.
 *
 * <p>Tests unitaires : ils n'ont besoin ni de Keycloak ni d'une base, et ils tombent
 * immédiatement si le format des claims change.
 */
class JetonsInvalidesTests {

	private static final String AUDIENCE = "supportdesk-api";

	@Test
	@DisplayName("un jeton émis pour une autre audience est refusé")
	void audienceEtrangere_estRefusee() {
		// Le realm supportdesk peut émettre des jetons pour plusieurs clients. Vérifier
		// seulement l'émetteur laisserait n'importe lequel d'entre eux ouvrir cette API.
		var resultat = new ValidateurAudience(AUDIENCE).validate(jeton(List.of("une-autre-api")));

		assertThat(resultat.hasErrors()).isTrue();
	}

	@Test
	@DisplayName("un jeton émis pour cette audience est accepté")
	void audienceAttendue_estAcceptee() {
		assertThat(new ValidateurAudience(AUDIENCE).validate(jeton(List.of(AUDIENCE))).hasErrors())
				.isFalse();
	}

	@Test
	@DisplayName("les rôles sont lus dans realm_access.roles, là où Keycloak les met")
	void roles_lusDansRealmAccess() {
		Jwt jwt = Jwt.withTokenValue("x").header("alg", "RS256")
				.claim("preferred_username", "carol")
				.claim("realm_access", Map.of("roles", List.of("ADMIN", "AGENT", "offline_access")))
				.issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
				.build();

		var autorites = new ConvertisseurAutorites().convert(jwt).getAuthorities().stream()
				.map(GrantedAuthority::getAuthority).toList();

		// Les rôles techniques de Keycloak (offline_access, uma_authorization) ne sont pas
		// traduits : ils n'ont aucun sens ici et pollueraient les règles.
		assertThat(autorites).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_AGENT");
	}

	@Test
	@DisplayName("un jeton sans realm_access ne donne aucune autorité")
	void sansRealmAccess_aucuneAutorite() {
		Jwt jwt = Jwt.withTokenValue("x").header("alg", "RS256")
				.claim("preferred_username", "inconnu")
				.issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
				.build();

		assertThat(new ConvertisseurAutorites().convert(jwt).getAuthorities()).isEmpty();
	}

	private static Jwt jeton(List<String> audience) {
		return Jwt.withTokenValue("x").header("alg", "RS256")
				.claim("aud", audience)
				.issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
				.build();
	}
}
