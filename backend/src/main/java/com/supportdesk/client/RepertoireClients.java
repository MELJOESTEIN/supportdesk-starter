package com.supportdesk.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

/**
 * Accès aux fiches clients, par lot.
 *
 * <p>Le point de cette classe est {@link #parReferences} : une liste de tickets touche
 * quelques comptes distincts, jamais autant que de lignes. Résoudre référence par référence
 * est un N+1 qui traverse le réseau — le même défaut que le N+1 SQL, en cent fois plus cher.
 *
 * <p>Le CRM n'offre pas d'opération « donne-moi ces huit fiches ». On fait donc un appel par
 * référence <b>distincte</b>, servi par le cache dès le deuxième passage. C'est la meilleure
 * chose à faire avec ce contrat-là : on ne réécrit pas le legacy.
 */
@Service
public class RepertoireClients {

	private final CrmClientSoap crm;

	public RepertoireClients(CrmClientSoap crm) {
		this.crm = crm;
	}

	public ClientCrm parReference(String clientRef) {
		return this.crm.parReference(clientRef);
	}

	public List<ClientCrm> rechercher(String motif) {
		return this.crm.rechercher(motif);
	}

	/**
	 * Résout un lot de références.
	 *
	 * <p>Une référence introuvable ne fait pas échouer le lot : elle est simplement absente
	 * du résultat. Un ticket dont le compte a disparu du CRM doit rester affichable.
	 */
	public Map<String, ClientCrm> parReferences(Set<String> references) {
		Map<String, ClientCrm> resultat = new LinkedHashMap<>();

		for (String reference : references) {
			try {
				resultat.put(reference, this.crm.parReference(reference));
			}
			catch (ClientCrmInconnuException ex) {
				// Absent du CRM : on continue. L'appelant affichera la référence brute.
			}
		}
		return resultat;
	}
}
