package com.supportdesk.ticket;

import java.net.URI;
import java.util.List;

import com.supportdesk.core.PageReponse;
import com.supportdesk.securite.UtilisateurCourant;
import com.supportdesk.ticket.TicketDtos.ModificationTicket;
import com.supportdesk.ticket.TicketDtos.NouveauCommentaire;
import com.supportdesk.ticket.TicketDtos.NouveauTicket;
import com.supportdesk.ticket.TicketDtos.OptionClient;
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
 * <p>L'identité de l'appelant vient de {@link UtilisateurCourant}, construit à partir du
 * jeton. Aucun paramètre de requête ne porte plus d'identité : {@code crmClientRef},
 * {@code auteurUsername} et {@code inclureInternes} ont disparu de la signature — ils
 * décidaient de ce que l'appelant avait le droit d'obtenir, ce qui n'appartient pas au
 * client.
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

	private final TicketService service;

	public TicketController(TicketService service) {
		this.service = service;
	}

	/**
	 * Liste paginée.
	 *
	 * <p>Le périmètre vient de {@code utilisateur.perimetre()} : la référence CRM du jeton
	 * pour un client, {@code null} pour un agent. Un {@code ?crmClientRef=} envoyé par
	 * l'appelant reste accepté comme <b>filtre</b>, mais il ne peut que restreindre à
	 * l'intérieur du périmètre — le service s'en assure.
	 */
	@GetMapping
	public PageReponse<TicketResume> lister(
			UtilisateurCourant utilisateur,
			@RequestParam(required = false) String crmClientRef,
			@RequestParam(required = false) List<StatutTicket> statuts,
			@RequestParam(required = false) String assigneA,
			@RequestParam(required = false) String recherche,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int taille) {

		FiltreTickets filtre = new FiltreTickets(crmClientRef, statuts, assigneA, recherche);
		PageRequest pagination = PageRequest.of(page, Math.min(taille, 200),
				Sort.by(Sort.Direction.DESC, "derniereActiviteLe"));

		return this.service.lister(filtre, utilisateur, pagination);
	}

	/**
	 * Options du filtre « Client » de la file agent.
	 *
	 * <p>Déclaré <b>avant</b> {@code /{id}} : Spring préfère de toute façon un segment
	 * littéral à une variable de chemin, mais l'ordre de lecture évite la question.
	 */
	@GetMapping("/clients")
	public List<OptionClient> clientsDeLaFile() {
		return this.service.clientsDeLaFile();
	}

	@GetMapping("/{id}")
	public TicketDetail detail(@PathVariable Long id, UtilisateurCourant utilisateur) {
		return this.service.detail(id, utilisateur);
	}

	@PostMapping
	public ResponseEntity<TicketDetail> creer(@Valid @RequestBody NouveauTicket demande,
			UtilisateurCourant utilisateur) {
		TicketDetail cree = this.service.creer(demande, utilisateur);
		return ResponseEntity.created(URI.create("/api/tickets/" + cree.id())).body(cree);
	}

	@PostMapping("/{id}/commentaires")
	public TicketDetail commenter(@PathVariable Long id, @Valid @RequestBody NouveauCommentaire demande,
			UtilisateurCourant utilisateur) {
		return this.service.commenter(id, demande, utilisateur);
	}

	@PatchMapping("/{id}")
	public TicketDetail modifier(@PathVariable Long id, @RequestBody ModificationTicket demande,
			UtilisateurCourant utilisateur) {
		return this.service.modifier(id, demande, utilisateur);
	}
}
