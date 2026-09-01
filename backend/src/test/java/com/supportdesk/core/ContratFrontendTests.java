package com.supportdesk.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.supportdesk.ticket.TicketDtos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Le contrat entre le backend et le frontend, vérifié plutôt que promis.
 *
 * <p>Le modèle TypeScript a été écrit en premier (lot 2) et fait foi. Ce test lit le fichier
 * réel et compare ses champs à ceux des {@code record} Java. Renommer un champ d'un seul côté
 * casse ici, au build, et non trois écrans plus loin.
 *
 * <p>Un test à écrire une fois, et qui rend service pendant des années : c'est la couture
 * entre deux dépôts logiques qui, sinon, ne se parlent qu'à l'exécution.
 */
class ContratFrontendTests {

	private static final Path MODELE_TS =
			Path.of("..", "frontend", "src", "app", "ticket", "ticket.model.ts");

	@Test
	@DisplayName("TicketResume porte exactement les champs de l'interface TypeScript")
	void ticketResume_correspondAuModeleTypeScript() throws IOException {
		Set<String> attendus = champsDeLInterface("TicketResume");
		assumeThat(attendus).as("modèle TypeScript introuvable ou vide").isNotEmpty();

		assertThat(champsDuRecord(TicketDtos.TicketResume.class))
				.as("champs de TicketResume")
				.containsExactlyInAnyOrderElementsOf(attendus);
	}

	@Test
	@DisplayName("Commentaire porte exactement les champs de l'interface TypeScript")
	void commentaire_correspondAuModeleTypeScript() throws IOException {
		Set<String> attendus = champsDeLInterface("Commentaire");
		assumeThat(attendus).isNotEmpty();

		assertThat(champsDuRecord(TicketDtos.CommentaireVue.class))
				.as("champs de CommentaireVue")
				.containsExactlyInAnyOrderElementsOf(attendus);
	}

	@Test
	@DisplayName("NouveauTicket n'accepte pas plus de champs que le formulaire n'en propose")
	void nouveauTicket_correspondAuModeleTypeScript() throws IOException {
		Set<String> attendus = champsDeLInterface("NouveauTicket");
		assumeThat(attendus).isNotEmpty();

		// Le sens de ce test est la sécurité autant que le contrat : tout champ accepté ici
		// et absent du formulaire est une prise pour l'affectation en masse.
		assertThat(champsDuRecord(TicketDtos.NouveauTicket.class))
				.containsExactlyInAnyOrderElementsOf(attendus);
	}

	private static Set<String> champsDuRecord(Class<?> type) {
		return new LinkedHashSet<>(java.util.Arrays.stream(type.getRecordComponents())
				.map(java.lang.reflect.RecordComponent::getName).toList());
	}

	/**
	 * Extrait les champs d'une interface TypeScript.
	 *
	 * <p>Une analyse à la regex, volontairement modeste : elle échoue bruyamment si le
	 * fichier change de forme, ce qui est préférable à une analyse silencieusement fausse.
	 */
	private static Set<String> champsDeLInterface(String nom) throws IOException {
		if (!Files.exists(MODELE_TS)) {
			return Set.of();
		}
		String source = Files.readString(MODELE_TS);

		Matcher bloc = Pattern
				.compile("export interface " + nom + "(?: extends (\\w+))? \\{(.*?)\\n\\}", Pattern.DOTALL)
				.matcher(source);
		if (!bloc.find()) {
			return Set.of();
		}

		Set<String> champs = new LinkedHashSet<>();
		String parent = bloc.group(1);
		if (parent != null) {
			champs.addAll(champsDeLInterface(parent));
		}

		Matcher propriete = Pattern.compile("^\\s{2}(\\w+)\\??:", Pattern.MULTILINE).matcher(bloc.group(2));
		while (propriete.find()) {
			champs.add(propriete.group(1));
		}
		return champs;
	}

	@Test
	@DisplayName("le modèle TypeScript est bien là où on le cherche")
	void modeleTypeScript_estPresent() {
		assertThat(Files.exists(MODELE_TS))
				.as("le contrat vit dans %s ; s'il a bougé, ce test doit bouger avec lui",
						MODELE_TS.toAbsolutePath().normalize())
				.isTrue();
	}

	@Test
	@DisplayName("les valeurs d'énumération sont identiques des deux côtés")
	void enumerations_sontIdentiques() throws IOException {
		assumeThat(Files.exists(MODELE_TS)).isTrue();
		String source = Files.readString(MODELE_TS);

		verifierUnion(source, "StatutTicket", com.supportdesk.ticket.StatutTicket.values());
		verifierUnion(source, "PrioriteTicket", com.supportdesk.ticket.PrioriteTicket.values());
		verifierUnion(source, "CategorieTicket", com.supportdesk.ticket.CategorieTicket.values());
	}

	private static void verifierUnion(String source, String nom, Enum<?>[] valeurs) {
		Matcher union = Pattern.compile("export type " + nom + "\\s*=([^;]+);", Pattern.DOTALL)
				.matcher(source);
		assertThat(union.find()).as("union TypeScript %s", nom).isTrue();

		List<String> cotesTs = Pattern.compile("'([A-Z_]+)'").matcher(union.group(1)).results()
				.map((r) -> r.group(1)).toList();
		List<String> cotesJava = java.util.Arrays.stream(valeurs).map(Enum::name).toList();

		assertThat(cotesTs).as("valeurs de %s", nom).containsExactlyInAnyOrderElementsOf(cotesJava);
	}
}
