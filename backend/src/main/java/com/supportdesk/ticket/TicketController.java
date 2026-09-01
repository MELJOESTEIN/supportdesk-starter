package com.supportdesk.ticket;

import java.net.URI;
import java.util.List;

import com.supportdesk.core.PageReponse;
import com.supportdesk.ticket.TicketDtos.ModificationTicket;
import com.supportdesk.ticket.TicketDtos.NouveauCommentaire;
import com.supportdesk.ticket.TicketDtos.NouveauTicket;
import com.supportdesk.ticket.TicketDtos.TicketDetail;
import com.supportdesk.ticket.TicketDtos.TicketResume;

import jakarta.validation.Valid;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API des tickets.
 *
 * <h2>Avertissement — dette de sécurité datée</h2>
 *
 * <p>Aucun de ces endpoints n'est authentifié, et {@code GET /api/tickets/{id}} renvoie le
 * ticket de n'importe quel compte. C'est <b>volontaire</b> : la faille BOLA est le support
 * de la démonstration du J2, et elle est refermée au lot 4 (voir {@code prompts/004}).
 * Jusque-là, l'API n'écoute que sur localhost et ne sert que des données fictives.
 *
 * <p>Ce qui est déjà en place, parce que le défaire coûterait plus cher que le faire : aucune
 * entité JPA exposée, et des DTO d'entrée qui n'acceptent ni statut, ni propriétaire, ni
 * assignation.
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

	private final TicketService service;

	public TicketController(TicketService service) {
		this.service = service;
	}

	/**
	 * Liste paginée, triée par dernière activité décroissante.
	 *
	 * <p>{@code crmClientRef} et {@code inclureInternes} sont ici des <b>paramètres de
	 * requête</b> : au lot 4 ils disparaissent, remplacés par les claims du jeton. C'est
	 * exactement le défaut que le J2 corrige — une valeur envoyée par l'appelant ne peut pas
	 * décider de ce qu'il a le droit de lire.
	 */
	@GetMapping
	public PageReponse<TicketResume> lister(
			@RequestParam(required = false) String crmClientRef,
			@RequestParam(required = false) List<StatutTicket> statuts,
			@RequestParam(required = false) String assigneA,
			@RequestParam(required = false) String recherche,
			@RequestParam(defaultValue = "false") boolean inclureInternes,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int taille) {

		FiltreTickets filtre = new FiltreTickets(crmClientRef, statuts, assigneA, recherche);
		PageRequest pagination = PageRequest.of(page, Math.min(taille, 200),
				Sort.by(Sort.Direction.DESC, "derniereActiviteLe"));

		return this.service.lister(filtre, inclureInternes, pagination);
	}

	@GetMapping("/{id}")
	public TicketDetail detail(@PathVariable Long id,
			@RequestParam(defaultValue = "false") boolean pourAgent) {
		return this.service.detail(id, pourAgent);
	}

	@PostMapping
	public ResponseEntity<TicketDetail> creer(@Valid @RequestBody NouveauTicket demande,
			@RequestParam String auteurUsername, @RequestParam String crmClientRef) {
		TicketDetail cree = this.service.creer(demande, auteurUsername, crmClientRef);
		return ResponseEntity.created(URI.create("/api/tickets/" + cree.id())).body(cree);
	}

	@PostMapping("/{id}/commentaires")
	public TicketDetail commenter(@PathVariable Long id, @Valid @RequestBody NouveauCommentaire demande,
			@RequestParam String auteurUsername, @RequestParam AuteurType auteurType) {
		return this.service.commenter(id, demande, auteurUsername, auteurType);
	}

	@PatchMapping("/{id}")
	public TicketDetail modifier(@PathVariable Long id, @RequestBody ModificationTicket demande,
			@RequestParam String auteurUsername) {
		return this.service.modifier(id, demande, auteurUsername);
	}
}
