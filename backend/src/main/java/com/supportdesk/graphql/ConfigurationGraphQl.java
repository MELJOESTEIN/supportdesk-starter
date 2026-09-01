package com.supportdesk.graphql;

import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.instrumentation.Instrumentation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bornes d'abus.
 *
 * <p><b>Rien n'est activé par défaut</b> au-delà des limites du parser. Une seule requête
 * profondément imbriquée — ou multipliée par alias — suffit sinon à écrouler la base :
 *
 * <pre>
 *   { tickets { contenu { commentaires { ... } } } }   répété cinquante fois par alias
 * </pre>
 *
 * <p>Ces deux instrumentations sont le minimum vital d'une API GraphQL exposée. Elles ne
 * remplacent pas une limitation de débit, qui se place au reverse proxy.
 */
@Configuration(proxyBeanMethods = false)
public class ConfigurationGraphQl {

	/** Le schéma le plus profond utile ici fait quatre niveaux. Dix laisse de la marge. */
	@Bean
	public Instrumentation profondeurMaximale() {
		return new MaxQueryDepthInstrumentation(10);
	}

	/**
	 * La complexité compte les champs à résoudre, alias compris.
	 *
	 * <p>C'est elle qui arrête l'amplification par alias, que la profondeur seule ne voit
	 * pas : cinquante fois le même champ sous cinquante noms reste plat.
	 */
	@Bean
	public Instrumentation complexiteMaximale() {
		return new MaxQueryComplexityInstrumentation(200);
	}
}
