package com.supportdesk.tableaudebord;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.supportdesk.ticket.StatutTicket;

public final class TableauDeBordDtos {

	private TableauDeBordDtos() {
	}

	public record Indicateur(String cle, String libelle, String valeur, String precision, boolean alerte) {
	}

	public record ActiviteJour(LocalDate jour, long crees, long resolus) {
	}

	public record RepartitionStatut(StatutTicket statut, long nombre) {
	}

	public record TableauDeBord(
			Instant arreteLe,
			String equipe,
			List<Indicateur> indicateurs,
			List<ActiviteJour> activite,
			List<RepartitionStatut> repartition,
			long totalPeriode) {
	}
}
