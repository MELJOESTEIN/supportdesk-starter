package com.supportdesk.ticket;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.supportdesk.agent.Agent;
import com.supportdesk.agent.AgentRepository;
import com.supportdesk.agent.AgentResume;
import com.supportdesk.core.ConfigurationSupportdesk;
import com.supportdesk.core.PageReponse;
import com.supportdesk.ticket.TicketDtos.CommentaireVue;
import com.supportdesk.ticket.TicketDtos.EvenementVue;
import com.supportdesk.ticket.TicketDtos.ModificationTicket;
import com.supportdesk.ticket.TicketDtos.NouveauCommentaire;
import com.supportdesk.ticket.TicketDtos.NouveauTicket;
import com.supportdesk.ticket.TicketDtos.TicketDetail;
import com.supportdesk.ticket.TicketDtos.TicketResume;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Règles métier des tickets.
 *
 * <h2>Ce qui manque ici, et pourquoi</h2>
 *
 * <p><b>Aucune vérification de propriétaire.</b> {@link #detail(Long, boolean)} renvoie
 * n'importe quel ticket à n'importe quel appelant, et {@link #lister} ne borne le périmètre
 * que si l'appelant a bien voulu passer une référence client.
 *
 * <p>Ce n'est pas un oubli : c'est le matériel de démonstration du J2. La faille est
 * exploitée puis corrigée au lot 4, où {@code crmClientRef} viendra du jeton et où le
 * propriétaire sera vérifié <b>dans cette classe</b>, avant que le DTO ne soit construit.
 * D'ici là, l'API n'est accessible que sur localhost et ne contient que des données fictives.
 */
@Service
@Transactional(readOnly = true)
public class TicketService {

	/** Cycle de vie : ce qui n'est pas ici est interdit. */
	private static final Map<StatutTicket, Set<StatutTicket>> TRANSITIONS = Map.of(
			StatutTicket.OUVERT, EnumSet.of(StatutTicket.EN_COURS, StatutTicket.RESOLU, StatutTicket.FERME),
			StatutTicket.EN_COURS, EnumSet.of(StatutTicket.RESOLU, StatutTicket.FERME),
			StatutTicket.RESOLU, EnumSet.of(StatutTicket.EN_COURS, StatutTicket.FERME),
			// Un ticket fermé ne se rouvre pas : le client en ouvre un nouveau.
			StatutTicket.FERME, EnumSet.noneOf(StatutTicket.class));

	private final TicketRepository tickets;

	private final CommentaireRepository commentaires;

	private final EvenementRepository evenements;

	private final AgentRepository agents;

	private final ConfigurationSupportdesk configuration;

	private final Clock horloge;

	public TicketService(TicketRepository tickets, CommentaireRepository commentaires,
			EvenementRepository evenements, AgentRepository agents,
			ConfigurationSupportdesk configuration, Clock horloge) {
		this.tickets = tickets;
		this.commentaires = commentaires;
		this.evenements = evenements;
		this.agents = agents;
		this.configuration = configuration;
		this.horloge = horloge;
	}

	public PageReponse<TicketResume> lister(FiltreTickets filtre, boolean inclureInternes, Pageable pageable) {
		Page<LigneTicket> page = this.tickets.lister(filtre.crmClientRefOuVide(), filtre.statutsOuTous(),
				filtre.assigneOuVide(), filtre.nonAssigne(), filtre.rechercheOuVide(), inclureInternes,
				this.horloge.instant(), pageable);

		return PageReponse.de(page).transformer(TicketResume::de);
	}

	/**
	 * Détail d'un ticket avec ses commentaires.
	 *
	 * @param pourAgent décide si les notes internes et le journal sont <b>chargés</b>. Le
	 * filtrage est fait par la requête, pas après : pour un client, une note interne n'est
	 * jamais lue en base, donc jamais sérialisable par accident.
	 */
	public TicketDetail detail(Long id, boolean pourAgent) {
		Ticket ticket = this.tickets.findWithAgentById(id).orElseThrow(() -> new TicketIntrouvableException(id));

		// Une seule lecture de la table agent pour tout l'appel. La version précédente en
		// faisait trois — une par section — et le compte variait selon que le dernier
		// intervenant était un agent (déjà en cache) ou un client (requête supplémentaire).
		// Mesuré : 7 requêtes avant, 4 après, et désormais constant.
		Map<String, String> nomsAgents = nomsDesAgents();

		List<CommentaireVue> fil = vuesDesCommentaires(id, pourAgent, nomsAgents);
		List<EvenementVue> journal = pourAgent ? vuesDesEvenements(id, nomsAgents) : List.of();

		return construireDetail(ticket, fil, journal, nomsAgents);
	}

	@Transactional
	public TicketDetail creer(NouveauTicket demande, String auteurUsername, String crmClientRef) {
		Instant maintenant = this.horloge.instant();
		Ticket ticket = new Ticket(prochaineReference(), demande.sujet(), demande.description(),
				demande.categorie(), crmClientRef, auteurUsername, maintenant,
				maintenant.plus(this.configuration.getSlaPremiereReponse()));

		ticket.ajouterCommentaire(auteurUsername, AuteurType.CLIENT, demande.description(),
				VisibiliteCommentaire.PUBLIC, maintenant);
		ticket.journaliser(TypeEvenement.CREATION, auteurUsername, "ticket créé par le client", maintenant);

		Ticket enregistre = this.tickets.save(ticket);
		return detail(enregistre.getId(), false);
	}

	@Transactional
	public TicketDetail commenter(Long id, NouveauCommentaire demande, String auteurUsername,
			AuteurType auteurType) {
		Ticket ticket = this.tickets.findWithAgentById(id).orElseThrow(() -> new TicketIntrouvableException(id));
		Instant maintenant = this.horloge.instant();

		ticket.ajouterCommentaire(auteurUsername, auteurType, demande.contenu(), demande.visibilite(),
				maintenant);

		TypeEvenement type = (demande.visibilite() == VisibiliteCommentaire.INTERNE)
				? TypeEvenement.NOTE_INTERNE : TypeEvenement.REPONSE_CLIENT;
		String detail = (demande.visibilite() == VisibiliteCommentaire.INTERNE)
				? "note interne ajoutée" : "réponse publiée";
		ticket.journaliser(type, auteurUsername, detail, maintenant);

		this.tickets.flush();
		return detail(id, auteurType == AuteurType.AGENT);
	}

	@Transactional
	public TicketDetail modifier(Long id, ModificationTicket demande, String auteurUsername) {
		Ticket ticket = this.tickets.findWithAgentById(id).orElseThrow(() -> new TicketIntrouvableException(id));
		Instant maintenant = this.horloge.instant();

		if (demande.statut() != null && demande.statut() != ticket.getStatut()) {
			StatutTicket depuis = ticket.getStatut();
			if (!TRANSITIONS.getOrDefault(depuis, Set.of()).contains(demande.statut())) {
				throw new TransitionInterditeException(depuis, demande.statut());
			}
			ticket.changerStatut(demande.statut());
			ticket.journaliser(TypeEvenement.CHANGEMENT_STATUT, auteurUsername,
					"statut passé à " + demande.statut(), maintenant);
		}

		if (demande.priorite() != null && demande.priorite() != ticket.getPriorite()) {
			ticket.changerPriorite(demande.priorite());
			ticket.journaliser(TypeEvenement.CHANGEMENT_PRIORITE, auteurUsername,
					"priorité passée à " + demande.priorite(), maintenant);
		}

		if (demande.assigneA() != null) {
			Agent agent = demande.assigneA().isBlank() ? null
					: this.agents.findById(demande.assigneA()).orElseThrow(
							() -> new IllegalArgumentException("Agent inconnu : " + demande.assigneA()));
			ticket.assigner(agent);
			ticket.journaliser(TypeEvenement.ASSIGNATION, auteurUsername,
					(agent == null) ? "désassigné" : "assigné à " + agent.getNomComplet(), maintenant);
		}

		this.tickets.flush();
		return detail(id, true);
	}

	private List<CommentaireVue> vuesDesCommentaires(Long ticketId, boolean inclureInternes,
			Map<String, String> nomsAgents) {
		List<Commentaire> fil = this.commentaires.parTicket(ticketId, inclureInternes);

		return fil.stream()
				.map((c) -> new CommentaireVue(c.getId(), c.getAuteurUsername(),
						nomsAgents.getOrDefault(c.getAuteurUsername(), c.getAuteurUsername()),
						c.getAuteurType(), c.getContenu(), c.getCreeLe(), c.getVisibilite()))
				.toList();
	}

	private List<EvenementVue> vuesDesEvenements(Long ticketId, Map<String, String> nomsAgents) {
		return this.evenements.findByTicketIdOrderByCreeLeDesc(ticketId).stream()
				.map((e) -> new EvenementVue(e.getId(), e.getType(),
						nomsAgents.getOrDefault(e.getAuteurUsername(), e.getAuteurUsername()), e.getDetail(),
						e.getCreeLe()))
				.toList();
	}

	/**
	 * Noms d'affichage des agents.
	 *
	 * <p>Une requête pour toute l'équipe, pas une par commentaire : la table compte quelques
	 * lignes, et c'est la différence entre une requête et N.
	 */
	private Map<String, String> nomsDesAgents() {
		return this.agents.findAll().stream()
				.collect(Collectors.toMap(Agent::getUsername, Agent::getNomComplet, (a, b) -> a));
	}

	private TicketDetail construireDetail(Ticket ticket, List<CommentaireVue> fil,
			List<EvenementVue> journal, Map<String, String> nomsAgents) {
		boolean slaDepasse = ticket.getPremiereReponseLe() == null && ticket.getEcheanceSlaLe() != null
				&& ticket.getEcheanceSlaLe().isBefore(this.horloge.instant());

		long messagesVisibles = fil.stream().filter((c) -> c.visibilite() == VisibiliteCommentaire.PUBLIC)
				.count();

		return new TicketDetail(ticket.getId(), ticket.getReference(), ticket.getSujet(),
				ticket.getStatut(), ticket.getPriorite(), ticket.getCrmClientRef(),
				// Renseigné au lot 5, quand le CRM sera branché.
				null,
				AgentResume.de(ticket.getAssigneA()), ticket.getCreeLe(), ticket.getDerniereActiviteLe(),
				nomDActivite(ticket, nomsAgents), messagesVisibles, slaDepasse, ticket.getDescription(),
				ticket.getCategorie(), fil, journal);
	}

	/**
	 * Nom d'affichage du dernier intervenant.
	 *
	 * <p>Résolu depuis la carte déjà chargée, pas par une requête : un client n'existe pas
	 * dans la table {@code agent}, et l'interroger pour lui coûtait une requête de plus —
	 * variable selon le ticket, ce qui rendait le compte imprévisible.
	 */
	private String nomDActivite(Ticket ticket, Map<String, String> nomsAgents) {
		String username = ticket.getDerniereActivitePar();
		if (username == null) {
			return null;
		}
		return nomsAgents.getOrDefault(username, username);
	}

	private String prochaineReference() {
		Function<Long, String> format = (n) -> "TCK-" + (4900 + n);
		return format.apply(this.tickets.count() + 1);
	}
}
