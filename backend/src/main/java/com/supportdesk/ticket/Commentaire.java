package com.supportdesk.ticket;

import java.time.Instant;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "commentaire")
public class Commentaire {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ticket_id", nullable = false)
	private Ticket ticket;

	@Column(name = "auteur_username", nullable = false, length = 64)
	private String auteurUsername;

	@Enumerated(EnumType.STRING)
	@Column(name = "auteur_type", nullable = false, length = 20)
	private AuteurType auteurType;

	@Column(name = "contenu", nullable = false, columnDefinition = "text")
	private String contenu;

	@Enumerated(EnumType.STRING)
	@Column(name = "visibilite", nullable = false, length = 20)
	private VisibiliteCommentaire visibilite;

	@Column(name = "cree_le", nullable = false)
	private Instant creeLe;

	protected Commentaire() {
		// requis par JPA
	}

	Commentaire(Ticket ticket, String auteurUsername, AuteurType auteurType, String contenu,
			VisibiliteCommentaire visibilite, Instant creeLe) {
		this.ticket = ticket;
		this.auteurUsername = auteurUsername;
		this.auteurType = auteurType;
		this.contenu = contenu;
		this.visibilite = visibilite;
		this.creeLe = creeLe;
	}

	public Long getId() {
		return this.id;
	}

	public String getAuteurUsername() {
		return this.auteurUsername;
	}

	public AuteurType getAuteurType() {
		return this.auteurType;
	}

	public String getContenu() {
		return this.contenu;
	}

	public VisibiliteCommentaire getVisibilite() {
		return this.visibilite;
	}

	public Instant getCreeLe() {
		return this.creeLe;
	}
}
