package com.supportdesk.core;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Réglages métier, sous le préfixe {@code supportdesk}. */
@ConfigurationProperties(prefix = "supportdesk")
public class ConfigurationSupportdesk {

	/** Délai contractuel de première réponse. Sert au drapeau « SLA dépassé ». */
	private Duration slaPremiereReponse = Duration.ofHours(2);

	public Duration getSlaPremiereReponse() {
		return this.slaPremiereReponse;
	}

	public void setSlaPremiereReponse(Duration slaPremiereReponse) {
		this.slaPremiereReponse = slaPremiereReponse;
	}
}
