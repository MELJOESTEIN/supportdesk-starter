package com.supportdesk.client;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpComponents5MessageSender;

/**
 * Client SOAP du CRM legacy.
 *
 * <h2>Timeouts</h2>
 *
 * <p>Explicites et courts. Le CRM répond en 400 ms par construction ; sans plafond, une
 * dépendance lente devient une panne — les threads s'accumulent en attente et c'est
 * l'application entière qui tombe, pas seulement l'écran qui affiche des raisons sociales.
 *
 * <h2>Durcissement XML</h2>
 *
 * <p>Le marshaller refuse les DTD et les entités externes. Un référentiel legacy est
 * exactement le genre de source qu'on ne croit pas sur parole : XXE et « billion laughs »
 * se traitent à la configuration, pas à la relecture.
 *
 * <p>Le {@code HttpComponents5MessageSender} de Spring WS 5.0 porte lui-même les timeouts
 * et le dimensionnement du pool : inutile d'assembler un {@code CloseableHttpClient} à la
 * main, ce que faisaient les versions précédentes.
 */
@Configuration(proxyBeanMethods = false)
public class ConfigurationCrm {

	@Bean
	public Jaxb2Marshaller marshallerCrm() {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath("com.supportdesk.client.contrat");
		marshaller.setSupportDtd(false);
		marshaller.setProcessExternalEntities(false);
		return marshaller;
	}

	@Bean
	public HttpComponents5MessageSender expediteurCrm(
			@Value("${supportdesk.crm.connect-timeout:2s}") Duration connectTimeout,
			@Value("${supportdesk.crm.read-timeout:5s}") Duration readTimeout) {

		HttpComponents5MessageSender expediteur = new HttpComponents5MessageSender();
		expediteur.setConnectionTimeout(connectTimeout);
		expediteur.setReadTimeout(readTimeout);
		// Borne la concurrence sortante : le CRM legacy n'encaisse pas cinquante appels
		// simultanés, et l'écrouler ne rendrait service à personne.
		expediteur.setMaxTotalConnections(20);
		return expediteur;
	}

	/**
	 * Le template est construit par le {@link WebServiceTemplateBuilder} de Boot, pas
	 * avec {@code new}.
	 *
	 * <p>Ce détail n'est pas cosmétique : le builder applique les {@code customizers} du
	 * contexte, et c'est par lui que {@code @AutoConfigureMockWebServiceServer} branche le
	 * serveur simulé des tests. Un template assemblé à la main n'est jamais intercepté, et
	 * les tests échouent sur « Further connection(s) expected » sans dire pourquoi.
	 */
	@Bean
	public WebServiceTemplate webServiceTemplateCrm(WebServiceTemplateBuilder builder,
			Jaxb2Marshaller marshallerCrm, HttpComponents5MessageSender expediteurCrm,
			@Value("${supportdesk.crm.url}") String url) {

		return builder.setDefaultUri(url)
				.setMarshaller(marshallerCrm)
				.setUnmarshaller(marshallerCrm)
				.messageSenders(expediteurCrm)
				.build();
	}
}
