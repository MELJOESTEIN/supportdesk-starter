package com.supportdesk.client;

/**
 * Fiche client, telle que l'application la manipule.
 *
 * <p>Volontairement distincte de la classe JAXB générée : le contrat du fournisseur ne
 * traverse pas l'application. Le jour où le CRM change de forme — ou est remplacé — seul
 * le traducteur bouge.
 */
public record ClientCrm(String clientRef, String raisonSociale, String siret, String contactEmail,
		String contactTel, boolean actif) {
}
