package com.supportdesk.tableaudebord;

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

	/**
	 * Seuils affichés sur les tuiles. Écrits ici et nulle part ailleurs : le libellé et la
	 * comparaison doivent bouger ensemble, sinon le tableau annonce un objectif et en
	 * applique un autre.
	 */
	private static final double SEUIL_SLA_POURCENT = 90.0;

	private static final Duration OBJECTIF_PREMIERE_REPONSE = Duration.ofHours(2);


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

		// Un indicateur qui affiche un objectif doit le comparer. « SLA RESPECTÉ 4,3 % » face
		// à « seuil contractuel 90 % » portait le même liseré gris qu'un compteur ordinaire :
		// le chiffre le plus grave du tableau était le moins visible. L'objectif n'était pas
		// un seuil, c'était une légende.
		Long medianeMinutes = medianePremiereReponseEnMinutes();
		boolean slaSousLeSeuil = avecReponse > 0 && (slaRespecte * 100.0) / avecReponse < SEUIL_SLA_POURCENT;
		boolean premiereReponseTropLente = medianeMinutes != null
				&& medianeMinutes > OBJECTIF_PREMIERE_REPONSE.toMinutes();

		List<Indicateur> indicateurs = List.of(
				new Indicateur("OUVERTS", "● TICKETS OUVERTS", String.valueOf(parStatut.get(StatutTicket.OUVERT)),
						"sur " + total + " tickets", false),
				new Indicateur("EN_COURS", "◑ EN COURS", String.valueOf(parStatut.get(StatutTicket.EN_COURS)),
						"en cours de traitement", false),
				new Indicateur("RESOLUS_7J", "✓ RÉSOLUS · 7 JOURS", String.valueOf(resolus7j),
						"sur les sept derniers jours", false),
				new Indicateur("PREMIERE_REPONSE_MEDIANE", "1RE RÉPONSE · MÉDIANE", medianePremiereReponse(),
						"objectif 2 h", premiereReponseTropLente),
				new Indicateur("SLA_RESPECTE", "SLA RESPECTÉ", pourcentage(slaRespecte, avecReponse),
						"seuil contractuel 90 %", slaSousLeSeuil),
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
			// Le driver PostgreSQL 42.7 renvoie un java.time.LocalDate pour une colonne date,
			// plus un java.sql.Date : le transtypage direct explose en ClassCastException.
			LocalDate jour = (ligne[0] instanceof LocalDate date) ? date
					: ((java.sql.Date) ligne[0]).toLocalDate();
			serie.add(new ActiviteJour(jour, ((Number) ligne[1]).longValue(), ((Number) ligne[2]).longValue()));
		}
		return serie;
	}

	/**
	 * Médiane, pas moyenne : un seul ticket oublié trois semaines déplace une moyenne et ne
	 * dit rien de l'expérience courante. La médiane, elle, décrit le cas typique.
	 *
	 * @return le délai en minutes, ou {@code null} si aucun ticket n'a encore de réponse
	 */
	private Long medianePremiereReponseEnMinutes() {
		List<Long> minutes = this.depot.delaisDePremiereReponse().stream()
				.map((ligne) -> Duration.between((Instant) ligne[0], (Instant) ligne[1]).toMinutes())
				.sorted()
				.toList();

		return minutes.isEmpty() ? null : minutes.get(minutes.size() / 2);
	}

	private String medianePremiereReponse() {
		Long mediane = medianePremiereReponseEnMinutes();
		if (mediane == null) {
			return "—";
		}
		return (mediane < 60) ? mediane + " min"
				: String.format(Locale.FRANCE, "%d h %02d", mediane / 60, mediane % 60);
	}

	private String pourcentage(long numerateur, long denominateur) {
		if (denominateur == 0) {
			return "—";
		}
		return String.format(Locale.FRANCE, "%.1f %%", (numerateur * 100.0) / denominateur);
	}
}
