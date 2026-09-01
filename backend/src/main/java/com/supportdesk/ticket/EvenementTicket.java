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

/** Journal d'un ticket — l'encart « JOURNAL » de l'écran agent. Réservé aux agents. */
@Entity
@Table(name = "evenement_ticket")
public class EvenementTicket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ticket_id", nullable = false)
	private Ticket ticket;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 32)
	private TypeEvenement type;

	@Column(name = "auteur_username", nullable = false, length = 64)
	private String auteurUsername;

	@Column(name = "detail", nullable = false, length = 255)
	private String detail;

	@Column(name = "cree_le", nullable = false)
	private Instant creeLe;

	protected EvenementTicket() {
		// requis par JPA
	}

	EvenementTicket(Ticket ticket, TypeEvenement type, String auteurUsername, String detail, Instant creeLe) {
		this.ticket = ticket;
		this.type = type;
		this.auteurUsername = auteurUsername;
		this.detail = detail;
		this.creeLe = creeLe;
	}

	public Long getId() {
		return this.id;
	}

	public TypeEvenement getType() {
		return this.type;
	}

	public String getAuteurUsername() {
		return this.auteurUsername;
	}

	public String getDetail() {
		return this.detail;
	}

	public Instant getCreeLe() {
		return this.creeLe;
	}
}
