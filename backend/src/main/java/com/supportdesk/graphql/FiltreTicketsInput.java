package com.supportdesk.graphql;

import java.util.List;

import com.supportdesk.ticket.StatutTicket;

/** L'input {@code FiltreTickets} du schéma. */
public record FiltreTicketsInput(List<StatutTicket> statuts, String crmClientRef, String assigneA,
		String recherche) {
}
