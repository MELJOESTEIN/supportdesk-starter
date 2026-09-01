package com.supportdesk.securite;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Jetons de test, calqués sur ceux qu'émet réellement le realm {@code supportdesk}.
 *
 * <p>Les claims sont ceux qu'on retrouve dans un vrai jeton — {@code preferred_username},
 * {@code crm_client_ref}, {@code realm_access.roles} — pour que les tests échouent si le
 * mapper Keycloak change de nom de claim.
 */
public final class Jetons {

	private Jetons() {
	}

	/** alice : CLIENT rattaché à CLI-0001. */
	public static JwtRequestPostProcessor alice() {
		return client("alice", "CLI-0001");
	}

	/** david : CLIENT rattaché à CLI-0002. */
	public static JwtRequestPostProcessor david() {
		return client("david", "CLI-0002");
	}

	public static JwtRequestPostProcessor client(String username, String crmClientRef) {
		return jwt()
				.jwt((jeton) -> jeton.claim("preferred_username", username)
						.claim("crm_client_ref", crmClientRef)
						.claim("realm_access", Map.of("roles", List.of("CLIENT")))
						.audience(List.of("supportdesk-api")))
				.authorities(new SimpleGrantedAuthority("ROLE_CLIENT"));
	}

	/** bob : AGENT, sans référence CRM — il n'appartient à aucun compte client. */
	public static JwtRequestPostProcessor bob() {
		return jwt()
				.jwt((jeton) -> jeton.claim("preferred_username", "bob")
						.claim("realm_access", Map.of("roles", List.of("AGENT")))
						.audience(List.of("supportdesk-api")))
				.authorities(new SimpleGrantedAuthority("ROLE_AGENT"));
	}

	/**
	 * Un CLIENT dont le jeton ne porte pas de référence CRM.
	 *
	 * <p>Cas réel : un utilisateur créé à la main dans Keycloak, sans l'attribut. Le défaut
	 * naturel serait de le traiter comme « pas de filtre », donc de tout lui montrer.
	 */
	public static JwtRequestPostProcessor clientSansReference() {
		return jwt()
				.jwt((jeton) -> jeton.claim("preferred_username", "orphelin")
						.claim("realm_access", Map.of("roles", List.of("CLIENT")))
						.audience(List.of("supportdesk-api")))
				.authorities(new SimpleGrantedAuthority("ROLE_CLIENT"));
	}
}
