package fr.acme.legacy.crm;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RepertoireClientsTests {

	private final RepertoireClients repertoire = new RepertoireClients();

	@Test
	void contientLesHuitFichesDontUnCompteInactif() {
		assertThat(this.repertoire.parMotif("")).hasSize(8);
		assertThat(this.repertoire.parReference("CLI-0008")).get()
				.extracting("actif").isEqualTo(false);
	}

	@Test
	void parReference_estInsensibleALaCasseEtAuxEspaces() {
		assertThat(this.repertoire.parReference("  cli-0002 ")).get()
				.extracting("raisonSociale").isEqualTo("Ateliers Sud");
	}

	@Test
	void parReference_inconnue_renvoieVide() {
		assertThat(this.repertoire.parReference("CLI-9999")).isEmpty();
	}

	@Test
	void latenceArtificielle_estDauMoins400ms() {
		LatenceLegacy latence = new LatenceLegacy();
		assertThat(latence.getLatence()).isGreaterThanOrEqualTo(Duration.ofMillis(400));

		long debut = System.nanoTime();
		latence.patienter();
		long ecoule = (System.nanoTime() - debut) / 1_000_000;

		// Si quelqu'un « optimise » le service, ce test tombe : la lenteur est le sujet.
		assertThat(ecoule).isGreaterThanOrEqualTo(390);
	}
}
