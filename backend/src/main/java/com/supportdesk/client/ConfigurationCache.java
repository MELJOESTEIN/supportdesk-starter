package com.supportdesk.client;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Cache en mémoire, pour ne pas rappeler le CRM (400 ms) sur la même référence.
 *
 * <p>{@code @EnableCaching} vit ici plutôt que sur la classe d'application : posé sur le
 * {@code @SpringBootApplication}, il s'applique aussi aux tranches de test
 * ({@code @WebMvcTest}), qui ne chargent pas l'autoconfiguration du cache — et le contexte
 * échoue alors sur « No qualifying bean of type CacheManager », sans rapport visible avec
 * le contrôleur testé.
 *
 * <p>Un cache distribué serait de l'ingénierie en avance sur le besoin : une seule instance,
 * des données publiques d'entreprise, une expiration de soixante secondes.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class ConfigurationCache {
}
