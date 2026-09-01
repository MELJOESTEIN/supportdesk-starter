package com.supportdesk.core;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import com.supportdesk.ticket.TicketIntrouvableException;
import com.supportdesk.ticket.TransitionInterditeException;

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

	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail argumentInvalide(IllegalArgumentException ex) {
		ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		probleme.setType(URI.create(BASE + "argument-invalide"));
		probleme.setTitle("Argument invalide");
		return probleme;
	}
}
