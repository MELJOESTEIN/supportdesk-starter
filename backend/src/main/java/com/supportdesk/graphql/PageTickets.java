package com.supportdesk.graphql;

import java.util.List;

/** Le type {@code PageTickets} du schéma. */
public record PageTickets(List<VueTicket> contenu, int page, int taille, long total, int totalPages) {
}
