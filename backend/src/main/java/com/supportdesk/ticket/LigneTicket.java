package com.supportdesk.ticket;

import java.time.Instant;

/**
 * Projection plate d'une ligne de liste.
 *
 * <p>Elle est construite directement par la requête : ni entité chargée, ni association
 * parcourue, donc <b>une seule requête SQL</b> quelle que soit la taille de la page. Le
 * nombre de messages et le drapeau SLA sont calculés par la base, pas en mémoire.
 */
public record LigneTicket(
		Long id,
		String reference,
		String sujet,
		StatutTicket statut,
		PrioriteTicket priorite,
		String crmClientRef,
		String assigneUsername,
		String assigneNom,
		String assigneNiveau,
		String assigneEquipe,
		Instant creeLe,
		Instant derniereActiviteLe,
		String derniereActivitePar,
		String derniereActiviteNomAgent,
		long nombreMessages,
		boolean slaDepasse) {
}
