package com.supportdesk.graphql;

import java.time.Instant;

import com.supportdesk.ticket.PrioriteTicket;
import com.supportdesk.ticket.StatutTicket;

/**
 * Le type {@code Ticket} du schéma, côté Java.
 *
 * <p>C'est un {@code record} — donc {@code equals} et {@code hashCode} par valeur — parce que
 * {@code @BatchMapping} utilise l'objet source comme <b>clé de map</b>. Une classe sans
 * égalité structurelle y produit des résultats muets : le lot revient, aucune clé ne
 * correspond, et tous les champs valent {@code null} sans la moindre erreur.
 */
public record VueTicket(
		Long id,
		String reference,
		String sujet,
		StatutTicket statut,
		PrioriteTicket priorite,
		String crmClientRef,
		String assigneUsername,
		Instant creeLe,
		Instant derniereActiviteLe,
		long nombreMessages,
		boolean slaDepasse) {
}
