package com.supportdesk.tableaudebord;

import java.sql.Date;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.supportdesk.tableaudebord.TableauDeBordDtos.ActiviteJour;
import com.supportdesk.tableaudebord.TableauDeBordDtos.Indicateur;
import com.supportdesk.tableaudebord.TableauDeBordDtos.RepartitionStatut;
import com.supportdesk.tableaudebord.TableauDeBordDtos.TableauDeBord;
import com.supportdesk.ticket.StatutTicket;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Agrégats de l'écran 04. Réservé aux agents (contrôlé au lot 4). */
@Service
@Transactional(readOnly = true)
public class TableauDeBordService {

	private final TableauDeBordRepository depot;

	private final Clock horloge;

	public TableauDeBordService(TableauDeBordRepository depot, Clock horloge) {
		this.depot = depot;
		this.horloge = horloge;
	}

	public TableauDeBord construire(int jours) {
		Instant maintenant = this.horloge.instant();

		Map<StatutTicket, Long> parStatut = new EnumMap<>(StatutTicket.class);
		for (StatutTicket statut : StatutTicket.values()) {
			parStatut.put(statut, 0L);
		}
		for (Object[] ligne : this.depot.compterParStatut()) {
			parStatut.put((StatutTicket) ligne[0], (Long) ligne[1]);
		}

		long total = parStatut.values().stream().mapToLong(Long::longValue).sum();
		long resolus7j = this.depot.compterResolusDepuis(maintenant.minus(7, ChronoUnit.DAYS));
		long sansReponse48h = this.depot.compterSansReponseAvant(maintenant.minus(48, ChronoUnit.HOURS));
		long avecReponse = this.depot.compterAvecPremiereReponse();
		long slaRespecte = this.depot.compterSlaRespecte();

		List<Indicateur> indicateurs = List.of(
				new Indicateur("OUVERTS", "● TICKETS OUVERTS", String.valueOf(parStatut.get(StatutTicket.OUVERT)),
						"sur " + total + " tickets", false),
				new Indicateur("EN_COURS", "◑ EN COURS", String.valueOf(parStatut.get(StatutTicket.EN_COURS)),
						"en cours de traitement", false),
				new Indicateur("RESOLUS_7J", "✓ RÉSOLUS · 7 JOURS", String.valueOf(resolus7j),
						"sur les sept derniers jours", false),
				new Indicateur("PREMIERE_REPONSE_MEDIANE", "1RE RÉPONSE · MÉDIANE", medianePremiereReponse(),
						"objectif 2 h", false),
				new Indicateur("SLA_RESPECTE", "SLA RESPECTÉ", pourcentage(slaRespecte, avecReponse),
						"seuil contractuel 90 %", false),
				new Indicateur("SANS_REPONSE_48H", "⚠ SANS RÉPONSE > 48 H", String.valueOf(sansReponse48h),
						"Voir la liste", sansReponse48h > 0));

		List<RepartitionStatut> repartition = parStatut.entrySet().stream()
				.map((e) -> new RepartitionStatut(e.getKey(), e.getValue()))
				.sorted((a, b) -> a.statut().compareTo(b.statut()))
				.toList();

		return new TableauDeBord(maintenant, "Facturation", indicateurs, activite(maintenant, jours),
				repartition, total);
	}

	private List<ActiviteJour> activite(Instant maintenant, int jours) {
		Instant debut = maintenant.minus(jours - 1L, ChronoUnit.DAYS);
		List<ActiviteJour> serie = new ArrayList<>();

		for (Object[] ligne : this.depot.activiteParJour(debut, maintenant)) {
			LocalDate jour = ((Date) ligne[0]).toLocalDate();
			serie.add(new ActiviteJour(jour, ((Number) ligne[1]).longValue(), ((Number) ligne[2]).longValue()));
		}
		return serie;
	}

	/**
	 * Médiane, pas moyenne : un seul ticket oublié trois semaines déplace une moyenne et ne
	 * dit rien de l'expérience courante. La médiane, elle, décrit le cas typique.
	 */
	private String medianePremiereReponse() {
		List<Long> minutes = this.depot.delaisDePremiereReponse().stream()
				.map((ligne) -> Duration.between((Instant) ligne[0], (Instant) ligne[1]).toMinutes())
				.sorted()
				.toList();

		if (minutes.isEmpty()) {
			return "—";
		}

		long mediane = minutes.get(minutes.size() / 2);
		return (mediane < 60) ? mediane + " min" : String.format(Locale.FRANCE, "%d h %02d", mediane / 60, mediane % 60);
	}

	private String pourcentage(long numerateur, long denominateur) {
		if (denominateur == 0) {
			return "—";
		}
		return String.format(Locale.FRANCE, "%.1f %%", (numerateur * 100.0) / denominateur);
	}
}
