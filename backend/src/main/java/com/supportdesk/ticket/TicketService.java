package com.supportdesk.ticket;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.supportdesk.agent.Agent;
import com.supportdesk.agent.AgentRepository;
import com.supportdesk.agent.AgentResume;
import com.supportdesk.client.ClientCrm;
import com.supportdesk.client.RepertoireClients;
import com.supportdesk.core.ConfigurationSupportdesk;
import com.supportdesk.core.PageReponse;
import com.supportdesk.securite.UtilisateurCourant;
import com.supportdesk.ticket.TicketDtos.CommentaireVue;
import com.supportdesk.ticket.TicketDtos.EvenementVue;
import com.supportdesk.ticket.TicketDtos.ModificationTicket;
import com.supportdesk.ticket.TicketDtos.NouveauCommentaire;
import com.supportdesk.ticket.TicketDtos.NouveauTicket;
import com.supportdesk.ticket.TicketDtos.OptionClient;
import com.supportdesk.ticket.TicketDtos.TicketDetail;
import com.supportdesk.ticket.TicketDtos.TicketResume;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Règles métier des tickets.
 *
 * <h2>Autorisation au niveau objet</h2>
 *
 * <p>Toute lecture ou écriture d'un ticket désigné par son identifiant passe par
 * {@link #chargerAutorise}, qui compare le propriétaire du ticket à la référence CRM du
 * jeton. <b>Un seul endroit à relire</b> : une vérification recopiée dans cinq méthodes
 * finit toujours par manquer dans la sixième.
 *
 * <p>La vérification est ici, dans le service, et non dans le contrôleur : les resolvers
 * GraphQL du lot 5 appellent les mêmes méthodes et héritent donc de la même règle. Deux
 * implémentations d'une règle d'accès divergent tôt ou tard.
 *
 * <p>Avant cette correction, la liste était correctement bornée mais le détail ne vérifiait
 * rien — le cas le plus courant en production, et le plus trompeur : l'application avait
 * l'air sûre.
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

	private final RepertoireClients clients;

	private final ConfigurationSupportdesk configuration;

	private final Clock horloge;

	public TicketService(TicketRepository tickets, CommentaireRepository commentaires,
			EvenementRepository evenements, AgentRepository agents, RepertoireClients clients,
			ConfigurationSupportdesk configuration, Clock horloge) {
		this.tickets = tickets;
		this.commentaires = commentaires;
		this.evenements = evenements;
		this.agents = agents;
		this.clients = clients;
		this.configuration = configuration;
		this.horloge = horloge;
	}

	/**
	 * Options du filtre « Client » de la file agent.
	 *
	 * <p>Réservé aux agents, et pas seulement par confort d'affichage : c'est la liste des
	 * comptes clients de l'application. L'ouvrir à un CLIENT lui donnerait par la porte de
	 * derrière l'énumération que le CRM refuse par la porte de devant.
	 *
	 * <p>Les libellés sont résolus en <b>un</b> appel au CRM pour tout le lot, via
	 * {@link RepertoireClients#parReferences}. Une référence absente du CRM reste proposée
	 * sous sa forme brute : ses tickets existent, ils doivent rester filtrables.
	 *
	 * <p>Même dégradation que la liste : {@link #fichesOuVide} avale une panne du CRM et rend
	 * les références nues. Un référentiel injoignable ne doit pas emporter le filtre — sinon
	 * le back-office devient inutilisable pour une dépendance qui n'est qu'un confort
	 * d'affichage. C'est aussi ce qui permet à la CI de tourner sans le CRM.
	 */
	@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
	public List<OptionClient> clientsDeLaFile() {
		List<String> references = this.tickets.referencesClientsDeLaFile();
		Map<String, ClientCrm> fiches = fichesOuVide(new LinkedHashSet<>(references));

		return references.stream()
				.map((reference) -> new OptionClient(reference,
						fiches.containsKey(reference) ? fiches.get(reference).raisonSociale() : reference))
				.toList();
	}

	public PageReponse<TicketResume> lister(FiltreTickets filtre, UtilisateurCourant utilisateur,
			Pageable pageable) {
		// Le périmètre vient du jeton. Le filtre envoyé par l'appelant ne peut que restreindre
		// à l'intérieur : pour un client, sa propre référence l'emporte toujours.
		//
		// Un client sans référence est refusé, PAS traité comme « sans filtre » : sinon
		// un compte mal configuré verrait tous les tickets de tous les comptes.
		String reference;
		if (utilisateur.estAgent()) {
			reference = filtre.crmClientRefOuVide();
		}
		else {
			if (utilisateur.crmClientRef() == null) {
				throw new CompteNonRattacheException();
			}
			reference = utilisateur.crmClientRef();
		}

		Page<LigneTicket> page = this.tickets.lister(reference, filtre.statutsOuTous(),
				filtre.assigneOuVide(), filtre.nonAssigne(), filtre.rechercheOuVide(),
				utilisateur.estAgent(), this.horloge.instant(), pageable);

		PageReponse<TicketResume> resultat = PageReponse.de(page).transformer(TicketResume::de);
		return enrichirAvecLeCrm(resultat);
	}

	/**
	 * Renseigne la raison sociale depuis le CRM.
	 *
	 * <p><b>Le N+1 du lot 5, celui qui passe par le réseau.</b> Une page de cinquante
	 * tickets touche cinq ou six comptes distincts ; résoudre ligne par ligne ferait
	 * cinquante appels à 400 ms, soit vingt secondes. On collecte les références
	 * <b>distinctes</b>, on interroge une fois chacune, et le cache absorbe les pages
	 * suivantes.
	 *
	 * <p>Le CRM indisponible ne fait pas échouer la liste : les tickets s'affichent avec
	 * leur référence brute. Une dépendance d'affichage ne doit pas emporter la
	 * fonctionnalité principale.
	 */
	private PageReponse<TicketResume> enrichirAvecLeCrm(PageReponse<TicketResume> page) {
		Set<String> references = page.contenu().stream().map(TicketResume::crmClientRef)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		if (references.isEmpty()) {
			return page;
		}

		Map<String, ClientCrm> fiches = fichesOuVide(references);
		return page.transformer((ligne) -> ligne.avecRaisonSociale(
				raisonSociale(fiches, ligne.crmClientRef())));
	}

	private Map<String, ClientCrm> fichesOuVide(Set<String> references) {
		try {
			return this.clients.parReferences(references);
		}
		catch (RuntimeException ex) {
			return Map.of();
		}
	}

	private static String raisonSociale(Map<String, ClientCrm> fiches, String reference) {
		ClientCrm fiche = fiches.get(reference);
		return (fiche != null) ? fiche.raisonSociale() : null;
	}

	/**
	 * Détail d'un ticket avec ses commentaires.
	 *
	 * <p>Le propriétaire est vérifié avant toute lecture des commentaires : un ticket
	 * d'autrui ne donne lieu à aucune requête supplémentaire, et rien de son contenu
	 * n'atteint le sérialiseur.
	 */
	public TicketDetail detail(Long id, UtilisateurCourant utilisateur) {
		boolean pourAgent = utilisateur.estAgent();
		Ticket ticket = chargerAutorise(id, utilisateur);

		// Une seule lecture de la table agent pour tout l'appel. La version précédente en
		// faisait trois — une par section — et le compte variait selon que le dernier
		// intervenant était un agent (déjà en cache) ou un client (requête supplémentaire).
		// Mesuré : 7 requêtes avant, 4 après, et désormais constant.
		Map<String, String> nomsAgents = nomsDesAgents();

		List<CommentaireVue> fil = vuesDesCommentaires(id, pourAgent, nomsAgents);
		List<EvenementVue> journal = pourAgent ? vuesDesEvenements(id, nomsAgents) : List.of();

		return construireDetail(ticket, fil, journal, nomsAgents, pourAgent);
	}

	/**
	 * Création d'un ticket.
	 *
	 * <p>Le propriétaire est celui du jeton, jamais celui du corps de la requête. Un client
	 * ne peut donc pas déposer un ticket au nom d'un autre compte.
	 */
	@Transactional
	public TicketDetail creer(NouveauTicket demande, UtilisateurCourant utilisateur) {
		// Un agent n'appartient à aucun compte : le ticket créé n'aurait pas de propriétaire.
		// Refuser explicitement, plutôt que de laisser la contrainte NOT NULL produire un 500.
		if (utilisateur.crmClientRef() == null) {
			throw new CreationReserveeAuClientException();
		}

		String auteurUsername = utilisateur.username();
		String crmClientRef = utilisateur.crmClientRef();
		Instant maintenant = this.horloge.instant();
		Ticket ticket = new Ticket(prochaineReference(), demande.sujet(), demande.description(),
				demande.categorie(), crmClientRef, auteurUsername, maintenant,
				maintenant.plus(this.configuration.getSlaPremiereReponse()));

		ticket.ajouterCommentaire(auteurUsername, AuteurType.CLIENT, demande.description(),
				VisibiliteCommentaire.PUBLIC, maintenant);
		ticket.journaliser(TypeEvenement.CREATION, auteurUsername, "ticket créé par le client", maintenant);

		Ticket enregistre = this.tickets.save(ticket);
		return detail(enregistre.getId(), utilisateur);
	}

	/**
	 * Ajout d'un commentaire.
	 *
	 * <p>Un client ne peut pas écrire une note interne : le DTO l'accepte comme <i>demande</i>,
	 * le service décide de ce qui est <i>permis</i>. Sans ce contrôle, un client s'écrirait
	 * une note invisible à lui-même — et surtout lisible par toute l'équipe support.
	 */
	@Transactional
	public TicketDetail commenter(Long id, NouveauCommentaire demande, UtilisateurCourant utilisateur) {
		String auteurUsername = utilisateur.username();
		AuteurType auteurType = utilisateur.estAgent() ? AuteurType.AGENT : AuteurType.CLIENT;

		if (demande.visibilite() == VisibiliteCommentaire.INTERNE && !utilisateur.estAgent()) {
			throw new VisibiliteInterditeException();
		}

		// Écrire sur le ticket d'autrui est aussi grave que le lire : même vérification.
		Ticket ticket = chargerAutorise(id, utilisateur);
		Instant maintenant = this.horloge.instant();

		ticket.ajouterCommentaire(auteurUsername, auteurType, demande.contenu(), demande.visibilite(),
				maintenant);

		TypeEvenement type = (demande.visibilite() == VisibiliteCommentaire.INTERNE)
				? TypeEvenement.NOTE_INTERNE : TypeEvenement.REPONSE_CLIENT;
		String detail = (demande.visibilite() == VisibiliteCommentaire.INTERNE)
				? "note interne ajoutée" : "réponse publiée";
		ticket.journaliser(type, auteurUsername, detail, maintenant);

		this.tickets.flush();
		return detail(id, utilisateur);
	}

	@Transactional
	public TicketDetail modifier(Long id, ModificationTicket demande, UtilisateurCourant utilisateur) {
		String auteurUsername = utilisateur.username();
		// La chaîne de filtres réserve déjà PATCH aux agents. La vérification est répétée
		// ici parce qu'un appelant GraphQL ou un futur endpoint n'y passerait pas.
		Ticket ticket = chargerAutorise(id, utilisateur);
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
		return detail(id, utilisateur);
	}

	/**
	 * Charge un ticket <b>après</b> avoir vérifié que l'appelant y a droit.
	 *
	 * <p>La seule porte d'entrée vers un ticket désigné par son identifiant. Un agent passe
	 * partout ; un client ne passe que sur ses propres tickets ; un client sans référence CRM
	 * ne passe nulle part — un défaut permissif est la variante silencieuse de la même faille.
	 */
	private Ticket chargerAutorise(Long id, UtilisateurCourant utilisateur) {
		Ticket ticket = this.tickets.findWithAgentById(id)
				.orElseThrow(() -> new TicketIntrouvableException(id));

		if (utilisateur.estAgent()) {
			return ticket;
		}

		String perimetre = utilisateur.crmClientRef();
		if (perimetre == null) {
			throw new CompteNonRattacheException();
		}
		if (!ticket.appartientA(perimetre)) {
			throw new AccesRefuseException(id);
		}
		return ticket;
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
		return this.evenements.findByTicketIdOrderByCreeLeDescIdDesc(ticketId).stream()
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
			List<EvenementVue> journal, Map<String, String> nomsAgents, boolean pourAgent) {
		boolean slaDepasse = ticket.getPremiereReponseLe() == null && ticket.getEcheanceSlaLe() != null
				&& ticket.getEcheanceSlaLe().isBefore(this.horloge.instant());

		long messagesVisibles = fil.stream().filter((c) -> c.visibilite() == VisibiliteCommentaire.PUBLIC)
				.count();

		// Une seule résolution du compte pour les deux champs qui en dépendent : la carte
		// était déjà chargée deux fois quand le contact a été ajouté.
		ClientCrm fiche = fichesOuVide(Set.of(ticket.getCrmClientRef())).get(ticket.getCrmClientRef());

		return new TicketDetail(ticket.getId(), ticket.getReference(), ticket.getSujet(),
				ticket.getStatut(), ticket.getPriorite(), ticket.getCrmClientRef(),
				(fiche != null) ? fiche.raisonSociale() : null,
				(pourAgent && fiche != null) ? fiche.contactEmail() : null,
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
