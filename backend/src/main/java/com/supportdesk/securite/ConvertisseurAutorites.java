package com.supportdesk.securite;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Traduit les rôles Keycloak en autorités Spring Security.
 *
 * <p>Keycloak place les rôles de realm dans un objet imbriqué :
 * {@code {"realm_access": {"roles": ["CLIENT"]}}}. Aucune convention Spring ne va les y
 * chercher toute seule — d'où ce convertisseur, préféré à la propriété SpEL
 * {@code authorities-claim-expressions} de Boot 4.1 : dix lignes de Java se lisent, se
 * testent et s'expliquent en séance.
 *
 * <p>Le préfixe {@code ROLE_} est celui qu'attend {@code hasRole(...)}.
 */
public class ConvertisseurAutorites implements Converter<Jwt, AbstractAuthenticationToken> {

	private static final String CLAIM_REALM = "realm_access";

	private static final String CLAIM_ROLES = "roles";

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		return new JwtAuthenticationToken(jwt, autorites(jwt), jwt.getClaimAsString("preferred_username"));
	}

	@SuppressWarnings("unchecked")
	private Collection<GrantedAuthority> autorites(Jwt jwt) {
		Map<String, Object> realm = jwt.getClaimAsMap(CLAIM_REALM);
		if (realm == null) {
			return List.of();
		}

		Object roles = realm.get(CLAIM_ROLES);
		if (!(roles instanceof Collection<?> liste)) {
			return List.of();
		}

		// Keycloak ajoute ses propres rôles techniques (offline_access, uma_authorization) :
		// on ne les traduit pas, ils n'ont aucun sens pour cette application.
		return ((Collection<String>) liste).stream()
				.filter((role) -> role.equals("CLIENT") || role.equals("AGENT") || role.equals("ADMIN"))
				.map((role) -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
				.toList();
	}
}
