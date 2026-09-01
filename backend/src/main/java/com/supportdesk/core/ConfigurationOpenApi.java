package com.supportdesk.core;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Documentation OpenAPI — <b>hors production</b>.
 *
 * <p>Swagger UI sert à explorer l'API pendant le développement. Il ne sert pas à démontrer
 * la faille du J2 : son bouton « Authorize » masque le transport du jeton, alors que c'est
 * précisément ce qu'il faut voir. Cette démonstration se fait en {@code .http} et en
 * {@code curl}.
 *
 * <p>Le profil {@code prod} coupe {@code springdoc.api-docs} et {@code swagger-ui}.
 */
@Configuration(proxyBeanMethods = false)
@Profile("!prod")
public class ConfigurationOpenApi {

	@Bean
	public OpenAPI descriptionApi() {
		return new OpenAPI().info(new Info().title("SupportDesk API")
				.version("v1")
				.description("""
						API de la plateforme de tickets SupportDesk.

						Attention : à ce stade du parcours (lot 3), l'API n'est pas authentifiée
						et l'endpoint de détail renvoie le ticket de n'importe quel compte.
						C'est volontaire — voir prompts/003 et prompts/004."""));
	}
}
