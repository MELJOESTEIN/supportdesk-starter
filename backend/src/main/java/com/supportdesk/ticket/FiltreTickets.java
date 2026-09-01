package com.supportdesk.ticket;

import java.util.List;

/**
 * Critères de recherche d'une liste de tickets.
 *
 * <p>Aucun accesseur ne renvoie {@code null} : un paramètre JPQL nul n'a pas de type, et
 * PostgreSQL le reçoit alors en {@code bytea} — d'où le fameux
 * {@code function lower(bytea) does not exist}. Une chaîne vide, elle, est typée.
 *
 * <p>{@code crmClientRef} figure ici comme <b>critère</b>, pas comme frontière : au lot 4, le
 * périmètre viendra du jeton et ce champ ne pourra plus élargir ce qu'on a le droit de voir.
 */
public record FiltreTickets(
		String crmClientRef,
		List<StatutTicket> statuts,
		String assigneA,
		String recherche) {

	private static final String NON_ASSIGNE = "NON_ASSIGNE";

	public boolean nonAssigne() {
		return NON_ASSIGNE.equals(this.assigneA);
	}

	public String crmClientRefOuVide() {
		return vide(this.crmClientRef) ? "" : this.crmClientRef.trim();
	}

	/** Vide = pas de filtre. On passe alors tous les statuts : {@code IN ()} est invalide en SQL. */
	public List<StatutTicket> statutsOuTous() {
		return (this.statuts == null || this.statuts.isEmpty()) ? List.of(StatutTicket.values())
				: this.statuts;
	}

	public String assigneOuVide() {
		return (nonAssigne() || vide(this.assigneA)) ? "" : this.assigneA.trim();
	}

	public String rechercheOuVide() {
		return vide(this.recherche) ? "" : this.recherche.trim();
	}

	private static boolean vide(String valeur) {
		return valeur == null || valeur.isBlank();
	}
}
