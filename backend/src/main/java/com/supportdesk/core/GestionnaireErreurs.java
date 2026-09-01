package com.supportdesk.core;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import com.supportdesk.client.ClientCrmInconnuException;
import com.supportdesk.client.CrmIndisponibleException;
import com.supportdesk.client.CritereRechercheManquantException;
import com.supportdesk.client.FicheClientRefuseeException;
import com.supportdesk.ticket.AccesRefuseException;
import com.supportdesk.ticket.CompteNonRattacheException;
import com.supportdesk.ticket.CreationReserveeAuClientException;
import com.supportdesk.ticket.TicketIntrouvableException;
import com.supportdesk.ticket.TransitionInterditeException;
import com.supportdesk.ticket.VisibiliteInterditeException;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Erreurs de l'API, en {@code application/problem+json} (RFC 9457).
 *
 * <p>Deux règles pour les clients : le champ {@code type} est une URI stable, c'est sur elle
 * qu'un client branche sa logique ; {@code detail} est du texte destiné à un humain, il n'est
 * jamais analysé et peut changer.
 *
 * <p>{@code @Order(-1)} passe devant le gestionnaire par défaut de Boot, qui est à l'ordre 0.
 */
@RestControllerAdvice
@Order(-1)
public class GestionnaireErreurs {

	private static final String BASE = "https://supportdesk.local/erreurs/";

	@ExceptionHandler(TicketIntrouvableException.class)
	public ProblemDetail ticketIntrouvable(TicketIntrouvableException ex) {
		ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		probleme.setType(URI.create(BASE + "ticket-introuvable"));
		probleme.setTitle("Ticket introuvable");
		probleme.setProperty("ticketId", ex.getId());
		return probleme;
	}

	@ExceptionHandler(TransitionInterditeException.class)
	public ProblemDetail transitionInterdite(TransitionInterditeException ex) {
		ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		probleme.setType(URI.create(BASE + "transition-interdite"));
		probleme.setTitle("Transition de statut interdite");
		probleme.setProperty("depuis", ex.getDepuis());
		probleme.setProperty("vers", ex.getVers());
		return probleme;
	}

	/**
	 * Validation d'entrée.
	 *
	 * <p>Le détail par champ est renvoyé dans une propriété d'extension : un formulaire peut
	 * afficher l'erreur sous le bon champ sans analyser une phrase.
	 */
	/**
	 * Fault {@code CLIENT_INCONNU} du CRM.
	 *
	 * <p>404, pas 500 : le fournisseur a répondu, et sa réponse est « cette référence
	 * n'existe pas ». Un fault SOAP est une réponse, pas une panne.
	 */
	@ExceptionHandler(ClientCrmInconnuException.class)
	public ProblemDetail clientCrmInconnu(ClientCrmInconnuException ex) {
		ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		probleme.setType(URI.create(BASE + "client-crm-inconnu"));
		probleme.setTitle("Fiche client introuvable");
		probleme.setProperty("clientRef", ex.getClientRef());
		return probleme;
	}

	/** Fault {@code CRITERE_OBLIGATOIRE} : la demande était mal formée. */
	@ExceptionHandler(CritereRechercheManquantException.class)
	public ProblemDetail critereManquant(CritereRechercheManquantException ex) {
		ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		probleme.setType(URI.create(BASE + "critere-recherche-manquant"));
		probleme.setTitle("Critère de recherche obligatoire");
		return probleme;
	}

	/**
	 * Le CRM ne répond pas.
	 *
	 * <p>503 et non 500 : la panne n'est pas ici, et le client peut réessayer. Le message
	 * ne contient ni URL interne, ni trace : la forme du système d'en face ne regarde pas
	 * l'appelant.
	 */
	@ExceptionHandler(CrmIndisponibleException.class)
	public ProblemDetail crmIndisponible(CrmIndisponibleException ex) {
		ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
				"Le référentiel clients est momentanément indisponible");
		probleme.setType(URI.create(BASE + "crm-indisponible"));
		probleme.setTitle("Référentiel clients indisponible");
		return probleme;
	}

	@ExceptionHandler(FicheClientRefuseeException.class)
	public ProblemDetail ficheClientRefusee(FicheClientRefuseeException ex) {
		ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
		probleme.setType(URI.create(BASE + "fiche-client-refusee"));
		probleme.setTitle("Fiche client non accessible");
		return probleme;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail validation(MethodArgumentNotValidException ex) {
		ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"La requête contient des champs invalides");
		probleme.setType(URI.create(BASE + "requete-invalide"));
		probleme.setTitle("Requête invalide");

		Map<String, String> champs = new LinkedHashMap<>();
		for (FieldError erreur : ex.getBindingResult().getFieldErrors()) {
			champs.putIfAbsent(erreur.getField(), erreur.getDefaultMessage());
		}
		probleme.setProperty("champs", champs);
		return probleme;
	}

	/**
	 * Ticket appartenant à un autre compte.
	 *
	 * <p>Le corps ne contient <b>aucune donnée du ticket</b> : ni sujet, ni compte
	 * propriétaire. L'écran qui nomme le compte le fait à partir de ce que le client sait
	 * déjà, pas d'une information que l'API lui aurait livrée au passage.
	 */
	@ExceptionHandler(AccesRefuseException.class)
	public ProblemDetail accesRefuse(AccesRefuseException ex) {
		ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
				"Ce ticket appartient à un autre compte");
		probleme.setType(URI.create(BASE + "ticket-autre-compte"));
		probleme.setTitle("Accès non autorisé");
		return probleme;
	}

	@ExceptionHandler(CompteNonRattacheException.class)
	public ProblemDetail compteNonRattache(CompteNonRattacheException ex) {
		ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
		probleme.setType(URI.create(BASE + "compte-non-rattache"));
		probleme.setTitle("Compte non rattaché");
		return probleme;
	}

	@ExceptionHandler(CreationReserveeAuClientException.class)
	public ProblemDetail creationReserveeAuClient(CreationReserveeAuClientException ex) {
		ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
		probleme.setType(URI.create(BASE + "creation-reservee-au-client"));
		probleme.setTitle("Création réservée aux clients");
		return probleme;
	}

	@ExceptionHandler(VisibiliteInterditeException.class)
	public ProblemDetail visibiliteInterdite(VisibiliteInterditeException ex) {
		ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
		probleme.setType(URI.create(BASE + "visibilite-interdite"));
		probleme.setTitle("Visibilité non autorisée");
		return probleme;
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail argumentInvalide(IllegalArgumentException ex) {
		ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		probleme.setType(URI.create(BASE + "argument-invalide"));
		probleme.setTitle("Argument invalide");
		return probleme;
	}
}
