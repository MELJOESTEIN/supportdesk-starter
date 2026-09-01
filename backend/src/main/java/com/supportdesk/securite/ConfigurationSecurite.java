package com.supportdesk.securite;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Resource server OAuth2 sur le realm {@code supportdesk}.
 *
 * <p>Spring Security 7 : DSL lambda uniquement, {@code and()} et {@code authorizeRequests}
 * ont disparu.
 *
 * <h2>Ce que cette configuration fait, et ce qu'elle ne fait pas</h2>
 *
 * <p>Elle décide <b>qui est authentifié</b> et <b>quelles fonctions</b> lui sont ouvertes
 * (OWASP API2). Elle ne décide pas <b>quelles données</b> il peut lire : ça, c'est
 * l'autorisation au niveau objet (OWASP API1), et elle se fait dans le service, au plus près
 * de la donnée. Une règle de chaîne de filtres ne peut pas savoir à qui appartient le
 * ticket 42.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class ConfigurationSecurite {

	private final String issuer;

	private final String jwkSetUri;

	private final String audience;

	private final List<String> originesAutorisees;

	public ConfigurationSecurite(
			@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
			@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri,
			@Value("${supportdesk.audience}") String audience,
			@Value("${supportdesk.origines-autorisees}") List<String> originesAutorisees) {
		this.issuer = issuer;
		this.jwkSetUri = jwkSetUri;
		this.audience = audience;
		this.originesAutorisees = originesAutorisees;
	}

	@Bean
	public SecurityFilterChain chaineApi(HttpSecurity http) throws Exception {
		return http
				.securityMatcher("/api/**", "/graphql/**")
				// API sans état, authentifiée par jeton porteur : aucune credential ambiante,
				// donc rien à protéger contre le CSRF. Ce serait faux pour une session cookie.
				.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
				.sessionManagement((s) -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests((a) -> a
						// Les rôles ouvrent des FONCTIONS. L'accès aux données d'un ticket
						// précis est vérifié dans TicketService.
						.requestMatchers("/api/tableau-de-bord/**").hasAnyRole("AGENT", "ADMIN")
						.requestMatchers("/api/agents/**").hasAnyRole("AGENT", "ADMIN")
						// La recherche dans le référentiel n'est pas ouverte aux clients :
						// le CRM refuse de se laisser énumérer, l'API ne doit pas offrir
						// ce qu'il refuse. La fiche unique est filtrée dans le contrôleur,
						// par comparaison au jeton.
						.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/clients")
						.hasAnyRole("AGENT", "ADMIN")
						.requestMatchers(HttpMethod.PATCH, "/api/tickets/**").hasAnyRole("AGENT", "ADMIN")
						// La liste des comptes clients de la file. Deux barrières plutôt qu'une :
						// celle-ci et @PreAuthorize sur TicketService#clientsDeLaFile. Une règle
						// d'URL se contourne par un chemin qu'on n'avait pas prévu ; une
						// annotation de méthode ne se contourne pas par l'URL.
						.requestMatchers(HttpMethod.GET, "/api/tickets/clients")
						.hasAnyRole("AGENT", "ADMIN")
						.requestMatchers("/graphql/**").hasAnyRole("AGENT", "ADMIN")
						.anyRequest().authenticated())
				.oauth2ResourceServer((oauth2) -> oauth2
						.jwt((jwt) -> jwt.jwtAuthenticationConverter(new ConvertisseurAutorites())))
				.build();
	}

	/** Actuator et la documentation restent hors du périmètre authentifié. */
	@Bean
	public SecurityFilterChain chaineTechnique(HttpSecurity http) throws Exception {
		return http
				.securityMatcher("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests((a) -> a.anyRequest().permitAll())
				.build();
	}

	/**
	 * Décodeur de jetons.
	 *
	 * <p>Deux vérifications, pas une : l'émetteur <b>et</b> l'audience. Sans le contrôle
	 * d'audience, un jeton émis par le même realm pour une autre application ouvrirait cette
	 * API — c'est l'erreur classique du resource server « qui marche ».
	 *
	 * <p>{@code jwk-set-uri} est distinct de l'issuer : en conteneur, le navigateur voit
	 * {@code localhost:8081} et le backend joint Keycloak par son nom de service. L'issuer
	 * inscrit dans le jeton reste celui du navigateur.
	 */
	@Bean
	public JwtDecoder decodeurJwt() {
		NimbusJwtDecoder decodeur = this.jwkSetUri.isBlank()
				? NimbusJwtDecoder.withIssuerLocation(this.issuer).build()
				: NimbusJwtDecoder.withJwkSetUri(this.jwkSetUri).build();

		decodeur.setJwtValidator(validateurs(this.issuer, this.audience));
		return decodeur;
	}

	/**
	 * La chaîne de validation réellement installée sur le décodeur.
	 *
	 * <p>Extraite du bean pour une seule raison : la rendre vérifiable. Tant qu'elle était
	 * écrite en ligne, un test ne pouvait qu'instancier son propre {@code ValidateurAudience}
	 * — et prouver que la classe fonctionne, jamais qu'elle est <b>branchée</b>. Retirer la
	 * ligne {@code new ValidateurAudience(...)} laissait tous les tests au vert.
	 *
	 * <p>C'est la même leçon que la garde de route oubliée dans {@code app.routes.ts} le
	 * 30 août : le défaut n'était pas dans la fonction, il était dans son câblage, et quatre
	 * tests passaient sans le voir.
	 */
	static OAuth2TokenValidator<Jwt> validateurs(String issuer, String audience) {
		return JwtValidators.createDefaultWithValidators(
				JwtValidators.createDefaultWithIssuer(issuer),
				new ValidateurAudience(audience));
	}

	/**
	 * CORS.
	 *
	 * <p>Origines listées une par une. Jamais {@code *} : combiné à des credentials, c'est la
	 * porte ouverte, et le navigateur le refuse de toute façon. En production, le front est
	 * servi par le même nginx que l'API et cette configuration ne sert plus à rien — raison
	 * de plus pour qu'elle reste étroite.
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(this.originesAutorisees);
		configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
		configuration.setAllowCredentials(false);
		configuration.setMaxAge(1800L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		source.registerCorsConfiguration("/graphql/**", configuration);
		return source;
	}
}
