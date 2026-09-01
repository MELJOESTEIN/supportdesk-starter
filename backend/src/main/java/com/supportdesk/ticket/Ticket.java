package com.supportdesk.ticket;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.supportdesk.agent.Agent;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Ticket de support.
 *
 * <p>Cette entité n'est <b>jamais</b> renvoyée par un contrôleur : elle porte des données
 * qu'un client ne doit pas voir (ses commentaires internes, entre autres) et un graphe
 * d'associations dont la sérialisation part en boucle. Toujours un DTO ou une projection.
 *
 * <p>Toutes les associations sont en {@code LAZY} : le chargement est décidé par la requête,
 * pas par la cartographie. C'est ce qui permet de choisir, cas par cas, entre une jointure
 * et un accès séparé — et c'est là que se joue le N+1.
 */
@Entity
@Table(name = "ticket")
public class Ticket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "reference", nullable = false, unique = true, length = 32)
	private String reference;

	@Column(name = "sujet", nullable = false, length = 80)
	private String sujet;

	@Column(name = "description", nullable = false, columnDefinition = "text")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "categorie", nullable = false, length = 20)
	private CategorieTicket categorie;

	@Enumerated(EnumType.STRING)
	@Column(name = "statut", nullable = false, length = 20)
	private StatutTicket statut;

	@Enumerated(EnumType.STRING)
	@Column(name = "priorite", nullable = false, length = 20)
	private PrioriteTicket priorite;

	/** Référence du compte propriétaire. Le contrôle d'accès du lot 4 porte sur elle. */
	@Column(name = "crm_client_ref", nullable = false, length = 32)
	private String crmClientRef;

	@Column(name = "auteur_username", nullable = false, length = 64)
	private String auteurUsername;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assigne_a")
	private Agent assigneA;

	@Column(name = "cree_le", nullable = false)
	private Instant creeLe;

	@Column(name = "derniere_activite_le", nullable = false)
	private Instant derniereActiviteLe;

	@Column(name = "derniere_activite_par", length = 64)
	private String derniereActivitePar;

	@Column(name = "echeance_sla_le")
	private Instant echeanceSlaLe;

	@Column(name = "premiere_reponse_le")
	private Instant premiereReponseLe;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("creeLe ASC")
	private List<Commentaire> commentaires = new ArrayList<>();

	@OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("creeLe DESC")
	private List<EvenementTicket> evenements = new ArrayList<>();

	protected Ticket() {
		// requis par JPA
	}

	public Ticket(String reference, String sujet, String description, CategorieTicket categorie,
			String crmClientRef, String auteurUsername, Instant creeLe, Instant echeanceSlaLe) {
		this.reference = reference;
		this.sujet = sujet;
		this.description = description;
		this.categorie = categorie;
		this.crmClientRef = crmClientRef;
		this.auteurUsername = auteurUsername;
		this.creeLe = creeLe;
		this.derniereActiviteLe = creeLe;
		this.derniereActivitePar = auteurUsername;
		this.echeanceSlaLe = echeanceSlaLe;
		this.statut = StatutTicket.OUVERT;
		this.priorite = PrioriteTicket.NORMALE;
	}

	public Commentaire ajouterCommentaire(String auteurUsername, AuteurType auteurType, String contenu,
			VisibiliteCommentaire visibilite, Instant quand) {
		Commentaire commentaire = new Commentaire(this, auteurUsername, auteurType, contenu, visibilite, quand);
		this.commentaires.add(commentaire);

		// Une note interne ne compte pas comme une réponse au client : elle ne doit ni
		// remonter le ticket dans la liste du client, ni satisfaire le SLA.
		if (visibilite == VisibiliteCommentaire.PUBLIC) {
			this.derniereActiviteLe = quand;
			this.derniereActivitePar = auteurUsername;
			if (auteurType == AuteurType.AGENT && this.premiereReponseLe == null) {
				this.premiereReponseLe = quand;
			}
		}
		return commentaire;
	}

	public void journaliser(TypeEvenement type, String auteurUsername, String detail, Instant quand) {
		this.evenements.add(new EvenementTicket(this, type, auteurUsername, detail, quand));
	}

	public void changerStatut(StatutTicket statut) {
		this.statut = statut;
	}

	public void changerPriorite(PrioriteTicket priorite) {
		this.priorite = priorite;
	}

	public void assigner(Agent agent) {
		this.assigneA = agent;
	}

	public boolean appartientA(String crmClientRef) {
		return this.crmClientRef.equals(crmClientRef);
	}

	public Long getId() {
		return this.id;
	}

	public String getReference() {
		return this.reference;
	}

	public String getSujet() {
		return this.sujet;
	}

	public String getDescription() {
		return this.description;
	}

	public CategorieTicket getCategorie() {
		return this.categorie;
	}

	public StatutTicket getStatut() {
		return this.statut;
	}

	public PrioriteTicket getPriorite() {
		return this.priorite;
	}

	public String getCrmClientRef() {
		return this.crmClientRef;
	}

	public String getAuteurUsername() {
		return this.auteurUsername;
	}

	public Agent getAssigneA() {
		return this.assigneA;
	}

	public Instant getCreeLe() {
		return this.creeLe;
	}

	public Instant getDerniereActiviteLe() {
		return this.derniereActiviteLe;
	}

	public String getDerniereActivitePar() {
		return this.derniereActivitePar;
	}

	public Instant getEcheanceSlaLe() {
		return this.echeanceSlaLe;
	}

	public Instant getPremiereReponseLe() {
		return this.premiereReponseLe;
	}

	public List<Commentaire> getCommentaires() {
		return this.commentaires;
	}

	public List<EvenementTicket> getEvenements() {
		return this.evenements;
	}
}
