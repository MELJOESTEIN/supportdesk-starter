package fr.acme.legacy.crm;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Latence artificielle appliquée à chaque opération.
 *
 * <p>Ce n'est pas un défaut : un référentiel legacy répond lentement, et l'appelant doit
 * en tenir compte (timeouts, accès par lot, cache). Retirer cette latence ferait disparaître
 * le problème que le consommateur est censé résoudre.
 */
@Component
@ConfigurationProperties(prefix = "legacy-crm")
public class LatenceLegacy {

	private Duration latence = Duration.ofMillis(400);

	public Duration getLatence() {
		return latence;
	}

	public void setLatence(Duration latence) {
		this.latence = latence;
	}

	public void patienter() {
		try {
			Thread.sleep(latence);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
}
