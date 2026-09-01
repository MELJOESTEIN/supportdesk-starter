package com.supportdesk.core;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Une horloge injectable plutôt que des {@code Instant.now()} disséminés.
 *
 * <p>Sans elle, tester « le SLA est dépassé » demande d'attendre, ou de figer le temps par
 * un artifice. Avec elle, un test fournit l'instant qu'il veut.
 */
@Configuration(proxyBeanMethods = false)
public class ConfigurationHorloge {

	@Bean
	public Clock horloge() {
		return Clock.systemUTC();
	}
}
