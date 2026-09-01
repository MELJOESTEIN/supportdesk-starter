package com.supportdesk.ticket;

import java.time.Instant;
import java.util.List;

import com.supportdesk.agent.AgentResume;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Les DTO du domaine ticket, alignés champ pour champ sur {@code ticket.model.ts}.
 *
 * <p>Un test de sérialisation vérifie cet alignement : renommer un champ ici sans le
 * renommer côté TypeScript casse le test, pas seulement l'écran.
 */
public final class TicketDtos {

	private TicketDtos() {
	}

	/** Ligne de tableau — écrans 01 et 05. */
	public record TicketResume(
			Long id,
			String reference,
			String sujet,
			StatutTicket statut,
			PrioriteTicket priorite,
			String crmClientRef,
			/** Vient du CRM legacy : null tant que le lot 5 ne l'a pas branché. */
			String clientRaisonSociale,
			AgentResume assigneA,
			Instant creeLe,
			Instant derniereActiviteLe,
			String derniereActivitePar,
			long nombreMessages,
			boolean slaDepasse) {

		/** Copie avec la raison sociale renseignée depuis le CRM (lot 5). */
		public TicketResume avecRaisonSociale(String raisonSociale) {
			return new TicketResume(this.id, this.reference, this.sujet, this.statut, this.priorite,
					this.crmClientRef, raisonSociale, this.assigneA, this.creeLe,
					this.derniereActiviteLe, this.derniereActivitePar, this.nombreMessages,
					this.slaDepasse);
		}

		public static TicketResume de(LigneTicket ligne) {
			AgentResume assigne = (ligne.assigneUsername() == null) ? null
					: new AgentResume(ligne.assigneUsername(), ligne.assigneNom(), ligne.assigneNiveau(),
							ligne.assigneEquipe());

			// Le nom d'un agent s'affiche en toutes lettres ; pour un client, on garde son
			// identifiant : son nom d'usage appartient au CRM, pas à cette base.
			String activitePar = (ligne.derniereActiviteNomAgent() != null)
					? ligne.derniereActiviteNomAgent() : ligne.derniereActivitePar();

			return new TicketResume(ligne.id(), ligne.reference(), ligne.sujet(), ligne.statut(),
					ligne.priorite(), ligne.crmClientRef(), null, assigne, ligne.creeLe(),
					ligne.derniereActiviteLe(), activitePar, ligne.nombreMessages(), ligne.slaDepasse());
		}
	}

	public record CommentaireVue(
			Long id,
			String auteurUsername,
			String auteurNom,
			AuteurType auteurType,
			String contenu,
			Instant creeLe,
			VisibiliteCommentaire visibilite) {
	}

	public record EvenementVue(Long id, TypeEvenement type, String auteurNom, String detail, Instant creeLe) {
	}

	/** Détail — écrans 02 et 06. {@code evenements} est vide pour un client. */
	public record TicketDetail(
			Long id,
			String reference,
			String sujet,
			StatutTicket statut,
			PrioriteTicket priorite,
			String crmClientRef,
			String clientRaisonSociale,
			/**
			 * Courriel de contact du compte, lu au CRM. {@code null} pour un CLIENT.
			 *
			 * <p>Non pas parce qu'il s'agirait d'un secret — c'est son propre courriel —
			 * mais parce que son écran ne l'affiche pas. Un champ qu'aucune interface ne
			 * montre n'a rien à faire sur le réseau : c'est la définition de l'exposition
			 * excessive de données (OWASP API3).
			 */
			String clientContact,
			AgentResume assigneA,
			Instant creeLe,
			Instant derniereActiviteLe,
			String derniereActivitePar,
			long nombreMessages,
			boolean slaDepasse,
			String description,
			CategorieTicket categorie,
			List<CommentaireVue> commentaires,
			List<EvenementVue> evenements) {
	}

	/**
	 * Création d'un ticket.
	 *
	 * <p>Ni {@code statut}, ni {@code priorite}, ni {@code crmClientRef}, ni {@code assigneA} :
	 * ces valeurs sont décidées par le serveur. Les accepter ici serait de l'affectation en
	 * masse — un client s'attribuerait son propre ticket ou le déclarerait résolu.
	 */
	public record NouveauTicket(
			@NotBlank(message = "Le sujet est obligatoire")
			@Size(max = 80, message = "Le sujet ne doit pas dépasser 80 caractères")
			String sujet,

			@NotNull(message = "La catégorie est obligatoire")
			CategorieTicket categorie,

			@NotBlank(message = "La description est obligatoire")
			@Size(max = 8000)
			String description) {
	}

	/**
	 * Ajout d'un commentaire.
	 *
	 * <p>{@code visibilite} est accepté ici parce qu'un agent doit pouvoir choisir. Le service
	 * refuse INTERNE à un client : le DTO dit ce qui est <i>demandable</i>, le service ce qui
	 * est <i>permis</i>.
	 */
	public record NouveauCommentaire(
			@NotBlank(message = "Le contenu est obligatoire")
			@Size(max = 8000)
			String contenu,

			@NotNull(message = "La visibilité est obligatoire")
			VisibiliteCommentaire visibilite) {
	}

	/** Mutations réservées aux agents. Tous les champs sont optionnels. */
	/**
	 * Option du filtre « Client » de la file agent.
	 *
	 * <p>Ce n'est pas la fiche CRM : ni SIRET, ni contact, ni téléphone. Juste de quoi
	 * remplir un menu déroulant. Renvoyer {@code ClientCrm} ici exposerait un référentiel
	 * entier pour afficher huit libellés.
	 */
	public record OptionClient(String reference, String raisonSociale) {
	}

	public record ModificationTicket(StatutTicket statut, PrioriteTicket priorite, String assigneA) {
	}
}
