package fr.acme.legacy.crm;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Component;

import fr.acme.legacy.crm.contrat.Client;

/**
 * Référentiel clients en mémoire. Le vrai système lit un DB2 des années 2000 ;
 * ici huit lignes en dur suffisent, y compris un compte inactif.
 */
@Component
public class RepertoireClients {

	private final List<Client> clients = List.of(
			client("CLI-0001", "Transports Nord", "48291736500017", "contact@transports-nord.fr", "+33 3 20 55 14 02", true),
			client("CLI-0002", "Ateliers Sud", "39284710200043", "accueil@ateliers-sud.fr", "+33 4 91 22 87 30", true),
			client("CLI-0003", "Atelier Vernet", "51937284600028", "camille.roussel@atelier-vernet.fr", "+33 1 45 78 09 61", true),
			client("CLI-0004", "Groupe Lauziere", "72648193500011", "support@groupe-lauziere.fr", "+33 5 61 33 20 74", true),
			client("CLI-0005", "Merieux et Fils", "63481927300056", "compta@merieux-fils.fr", "+33 4 72 41 66 18", true),
			client("CLI-0006", "Fromageries Bellart", "28471639200034", "sav@fromageries-bellart.fr", "+33 2 41 88 03 25", true),
			client("CLI-0007", "Imprimerie Kessler", "84019273600062", "atelier@imprimerie-kessler.fr", "+33 3 88 60 47 13", true),
			// Compte résilié : le contrat prévoit le cas, l'appelant doit le gérer.
			client("CLI-0008", "Cartonnages Vasseur", "17392846500029", "ancien@cartonnages-vasseur.fr", "+33 2 35 71 90 44", false));

	public Optional<Client> parReference(String clientRef) {
		return clients.stream()
				.filter(c -> c.getClientRef().equalsIgnoreCase(clientRef == null ? "" : clientRef.trim()))
				.findFirst();
	}

	public List<Client> parMotif(String motif) {
		String recherche = motif.trim().toLowerCase(Locale.ROOT);
		return clients.stream()
				.filter(c -> c.getRaisonSociale().toLowerCase(Locale.ROOT).contains(recherche))
				.toList();
	}

	private static Client client(String ref, String raisonSociale, String siret, String email, String tel, boolean actif) {
		Client c = new Client();
		c.setClientRef(ref);
		c.setRaisonSociale(raisonSociale);
		c.setSiret(siret);
		c.setContactEmail(email);
		c.setContactTel(tel);
		c.setActif(actif);
		return c;
	}
}
