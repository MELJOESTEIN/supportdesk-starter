package com.supportdesk.securite;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Vérifie que le jeton a bien été émis <b>pour cette API</b>.
 *
 * <p>Le realm {@code supportdesk} peut émettre des jetons pour plusieurs clients. Sans ce
 * contrôle, un jeton obtenu par une autre application du même realm — ou par un client public
 * quelconque — ouvrirait cette API. Vérifier l'émetteur ne suffit pas.
 */
public class ValidateurAudience implements OAuth2TokenValidator<Jwt> {

	private final String audienceAttendue;

	public ValidateurAudience(String audienceAttendue) {
		this.audienceAttendue = audienceAttendue;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt jeton) {
		if (jeton.getAudience().contains(this.audienceAttendue)) {
			return OAuth2TokenValidatorResult.success();
		}
		return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
				"Le jeton n'a pas été émis pour l'audience " + this.audienceAttendue, null));
	}
}
