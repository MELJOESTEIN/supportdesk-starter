package com.supportdesk.agent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Agent de support.
 *
 * <p>Les agents sont en base parce que l'application a besoin de leur nom d'affichage et de
 * leur équipe ; leur authentification, elle, reste dans Keycloak. Les clients, eux, ne sont
 * pas en base du tout : leur identité vient du CRM legacy.
 */
@Entity
@Table(name = "agent")
public class Agent {

	@Id
	@Column(name = "username", nullable = false, length = 64)
	private String username;

	@Column(name = "nom_complet", nullable = false, length = 128)
	private String nomComplet;

	@Column(name = "niveau", length = 32)
	private String niveau;

	@Column(name = "equipe", length = 64)
	private String equipe;

	@Column(name = "actif", nullable = false)
	private boolean actif = true;

	protected Agent() {
		// requis par JPA
	}

	public String getUsername() {
		return this.username;
	}

	public String getNomComplet() {
		return this.nomComplet;
	}

	public String getNiveau() {
		return this.niveau;
	}

	public String getEquipe() {
		return this.equipe;
	}

	public boolean isActif() {
		return this.actif;
	}
}
