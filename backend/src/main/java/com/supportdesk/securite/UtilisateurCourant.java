package com.supportdesk.securite;

import java.util.Set;

/**
 * L'appelant, tel que son jeton le décrit.
 *
 * <p>Rien ici ne vient de la requête : ni de l'URL, ni d'un paramètre, ni du corps. C'est la
 * réponse à la question 4 de {@code docs/produit.md} — « ce qui ne doit jamais dépendre du
 * client ». Un objet immuable construit à partir des seuls claims signés.
 */
public record UtilisateurCourant(String username, Set<String> roles, String crmClientRef) {

	public boolean estAgent() {
		return this.roles.contains("AGENT") || this.roles.contains("ADMIN");
	}

	public boolean estClient() {
		return this.roles.contains("CLIENT") && !estAgent();
	}

	/**
	 * Périmètre de lecture.
	 *
	 * <p>{@code null} pour un agent (il voit tous les comptes), la référence CRM du jeton
	 * pour un client. Aucun autre cas : un client sans référence est refusé en amont.
	 */
	public String perimetre() {
		return estAgent() ? null : this.crmClientRef;
	}
}
