package com.supportdesk.graphql;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.supportdesk.agent.Agent;
import com.supportdesk.agent.AgentRepository;
import com.supportdesk.agent.AgentResume;
import com.supportdesk.client.ClientCrm;
import com.supportdesk.client.RepertoireClients;
import com.supportdesk.securite.UtilisateurCourant;
import com.supportdesk.securite.UtilisateurCourantArgumentResolver;
import com.supportdesk.tableaudebord.TableauDeBordDtos.TableauDeBord;
import com.supportdesk.tableaudebord.TableauDeBordService;
import com.supportdesk.ticket.CommentaireRepository;
import com.supportdesk.ticket.FiltreTickets;
import com.supportdesk.ticket.PrioriteTicket;
import com.supportdesk.ticket.StatutTicket;
import com.supportdesk.ticket.TicketDtos.CommentaireVue;
import com.supportdesk.ticket.TicketDtos.ModificationTicket;
import com.supportdesk.ticket.TicketDtos.NouveauCommentaire;
import com.supportdesk.ticket.TicketDtos.TicketDetail;
import com.supportdesk.ticket.TicketDtos.TicketResume;
import com.supportdesk.ticket.TicketService;
import com.supportdesk.ticket.VisibiliteCommentaire;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * API GraphQL du back-office.
 *
 * <h2>Autorisation</h2>
 *
 * <p>{@code @PreAuthorize} est posé sur <b>chaque méthode concrète</b>, jamais sur une classe
 * parente : CVE-2026-41856 rappelle qu'une annotation portée par une superclasse d'un
 * {@code @Controller} est silencieusement ignorée. La règle de la chaîne de filtres sur
 * {@code /graphql} est une seconde barrière, pas la seule.
 *
 * <p>Toutes les opérations partagent une URL et une méthode HTTP : {@code POST /graphql} ne
 * distingue pas une lecture d'une mutation. L'autorisation doit donc vivre là où la donnée
 * est récupérée — et elle réutilise {@link TicketService}, qui porte déjà la vérification du
 * propriétaire. Deux implémentations d'une même règle divergent tôt ou tard.
 */
@Controller
public class TicketGraphQlController {

	private final TicketService tickets;

	private final TableauDeBordService tableauDeBord;

	private final RepertoireClients clients;

	private final AgentRepository agents;

	private final CommentaireRepository commentaires;

	public TicketGraphQlController(TicketService tickets, TableauDeBordService tableauDeBord,
			RepertoireClients clients, AgentRepository agents, CommentaireRepository commentaires) {
		this.tickets = tickets;
		this.tableauDeBord = tableauDeBord;
		this.clients = clients;
		this.agents = agents;
		this.commentaires = commentaires;
	}

	@QueryMapping
	@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
	public PageTickets tickets(@Argument FiltreTicketsInput filtre, @Argument int page,
			@Argument int taille) {
		FiltreTickets criteres = (filtre == null) ? new FiltreTickets(null, null, null, null)
				: new FiltreTickets(filtre.crmClientRef(), filtre.statuts(), filtre.assigneA(),
						filtre.recherche());

		var resultat = this.tickets.lister(criteres, courant(),
				PageRequest.of(page, Math.min(taille, 100),
						Sort.by(Sort.Direction.DESC, "derniereActiviteLe")));

		return new PageTickets(resultat.contenu().stream().map(TicketGraphQlController::vue).toList(),
				resultat.page(), resultat.taille(), resultat.total(), resultat.totalPages());
	}

	@QueryMapping
	@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
	public VueTicket ticket(@Argument Long id) {
		return vue(this.tickets.detail(id, courant()));
	}

	@QueryMapping
	@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
	public TableauDeBord tableauDeBord(@Argument int jours) {
		return this.tableauDeBord.construire(Math.clamp(jours, 1, 90));
	}

	@MutationMapping
	@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
	public VueTicket changerStatut(@Argument Long id, @Argument StatutTicket statut) {
		return vue(this.tickets.modifier(id, new ModificationTicket(statut, null, null), courant()));
	}

	@MutationMapping
	@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
	public VueTicket changerPriorite(@Argument Long id, @Argument PrioriteTicket priorite) {
		return vue(this.tickets.modifier(id, new ModificationTicket(null, priorite, null), courant()));
	}

	@MutationMapping
	@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
	public VueTicket assigner(@Argument Long id, @Argument String agent) {
		String cible = (agent == null) ? "" : agent;
		return vue(this.tickets.modifier(id, new ModificationTicket(null, null, cible), courant()));
	}

	@MutationMapping
	@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
	public VueTicket ajouterCommentaire(@Argument Long id, @Argument String contenu,
			@Argument VisibiliteCommentaire visibilite) {
		return vue(this.tickets.commenter(id, new NouveauCommentaire(contenu, visibilite), courant()));
	}

	/**
	 * Résolution par lot de la fiche CRM.
	 *
	 * <p><b>C'est le N+1 du J3, et il traverse le réseau.</b> Sans ce {@code @BatchMapping},
	 * une page de vingt-cinq tickets provoquerait vingt-cinq appels SOAP à 400 ms — dix
	 * secondes. Avec, un appel par référence <b>distincte</b>, soit six ou sept.
	 *
	 * <p>La signature dit tout : on reçoit la liste complète des tickets du niveau, pas un
	 * ticket à la fois. Le DataLoader qui l'appelle est créé <b>par requête</b> : aucun
	 * partage entre requêtes, donc aucune fuite de données entre appelants.
	 */
	@BatchMapping(typeName = "Ticket", field = "client")
	public Map<VueTicket, ClientCrm> client(List<VueTicket> lot) {
		Set<String> references = lot.stream().map(VueTicket::crmClientRef)
				.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

		Map<String, ClientCrm> fiches;
		try {
			fiches = this.clients.parReferences(references);
		}
		catch (RuntimeException ex) {
			// Le CRM indisponible ne doit pas vider l'écran : le champ vaut null, le reste
			// du ticket s'affiche. Un champ nullable dans le schéma, c'est fait pour ça.
			fiches = Map.of();
		}

		Map<VueTicket, ClientCrm> resultat = new LinkedHashMap<>();
		for (VueTicket ticket : lot) {
			ClientCrm fiche = fiches.get(ticket.crmClientRef());
			if (fiche != null) {
				resultat.put(ticket, fiche);
			}
		}
		return resultat;
	}

	/** Même principe pour l'agent assigné : une requête pour tout le lot, pas une par ligne. */
	@BatchMapping(typeName = "Ticket", field = "assigneA")
	public Map<VueTicket, AgentResume> assigneA(List<VueTicket> lot) {
		Set<String> usernames = lot.stream().map(VueTicket::assigneUsername)
				.filter((u) -> u != null)
				.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
		if (usernames.isEmpty()) {
			return Map.of();
		}

		Map<String, Agent> parUsername = new LinkedHashMap<>();
		this.agents.findAllById(usernames).forEach((a) -> parUsername.put(a.getUsername(), a));

		Map<VueTicket, AgentResume> resultat = new LinkedHashMap<>();
		for (VueTicket ticket : lot) {
			Agent agent = parUsername.get(ticket.assigneUsername());
			if (agent != null) {
				resultat.put(ticket, AgentResume.de(agent));
			}
		}
		return resultat;
	}

	/**
	 * Commentaires d'un ticket.
	 *
	 * <p>Volontairement pas un {@code @BatchMapping} : le fil n'est demandé que sur l'écran
	 * de détail, un ticket à la fois. Le regrouper compliquerait le code pour un gain nul —
	 * et le plafond de profondeur empêche d'en demander cinquante d'un coup.
	 */
	@SchemaMapping(typeName = "Ticket", field = "commentaires")
	public List<CommentaireVue> commentaires(VueTicket ticket) {
		UtilisateurCourant utilisateur = courant();
		return this.tickets.detail(ticket.id(), utilisateur).commentaires();
	}

	private static UtilisateurCourant courant() {
		return UtilisateurCourantArgumentResolver.courant();
	}

	private static VueTicket vue(TicketResume resume) {
		return new VueTicket(resume.id(), resume.reference(), resume.sujet(), resume.statut(),
				resume.priorite(), resume.crmClientRef(),
				(resume.assigneA() != null) ? resume.assigneA().username() : null, resume.creeLe(),
				resume.derniereActiviteLe(), resume.nombreMessages(), resume.slaDepasse());
	}

	private static VueTicket vue(TicketDetail detail) {
		return new VueTicket(detail.id(), detail.reference(), detail.sujet(), detail.statut(),
				detail.priorite(), detail.crmClientRef(),
				(detail.assigneA() != null) ? detail.assigneA().username() : null, detail.creeLe(),
				detail.derniereActiviteLe(), detail.nombreMessages(), detail.slaDepasse());
	}
}
