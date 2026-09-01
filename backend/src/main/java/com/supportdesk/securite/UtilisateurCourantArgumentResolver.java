package com.supportdesk.securite;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Injecte {@link UtilisateurCourant} dans les méthodes de contrôleur.
 *
 * <p>Le point de cette classe est de rendre impossible l'écriture d'un contrôleur qui
 * prendrait l'identité dans un paramètre de requête : la seule façon d'obtenir un
 * {@code UtilisateurCourant} est de le demander, et il est construit ici, à partir du jeton.
 */
public class UtilisateurCourantArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parametre) {
		return UtilisateurCourant.class.equals(parametre.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parametre, ModelAndViewContainer conteneur,
			NativeWebRequest requete, WebDataBinderFactory fabrique) {
		return courant();
	}

	/** Utilitaire pour le code hors contrôleur (services, resolvers GraphQL du lot 5). */
	public static UtilisateurCourant courant() {
		Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentification instanceof JwtAuthenticationToken jeton)) {
			return null;
		}

		Jwt jwt = jeton.getToken();
		Set<String> roles = jeton.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.filter((a) -> a.startsWith("ROLE_"))
				.map((a) -> a.substring("ROLE_".length()))
				.collect(Collectors.toSet());

		return new UtilisateurCourant(jwt.getClaimAsString("preferred_username"), roles,
				jwt.getClaimAsString("crm_client_ref"));
	}
}
