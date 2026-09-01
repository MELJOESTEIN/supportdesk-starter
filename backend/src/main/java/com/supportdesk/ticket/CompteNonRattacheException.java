package com.supportdesk.ticket;

/**
 * Un CLIENT dont le jeton ne porte aucune référence CRM.
 *
 * <p>Cas réel : un utilisateur créé à la main dans Keycloak sans l'attribut
 * {@code crmClientRef}. Le réflexe naturel — « pas de référence, donc pas de filtre » — lui
 * donne l'intégralité des tickets de tous les comptes. C'est la variante silencieuse de la
 * faille BOLA : elle ne demande même pas d'être exploitée.
 *
 * <p>Un compte mal configuré est refusé, jamais élargi.
 */
public class CompteNonRattacheException extends RuntimeException {

	public CompteNonRattacheException() {
		super("Votre compte n'est rattaché à aucun client");
	}
}
