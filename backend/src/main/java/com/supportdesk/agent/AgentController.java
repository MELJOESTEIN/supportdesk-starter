package com.supportdesk.agent;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liste des agents, pour alimenter le sélecteur « assigné à » du back-office.
 *
 * <p>Sécurité : cet endpoint révèle la composition de l'équipe support. Il sera réservé aux
 * rôles AGENT et ADMIN au lot 4 ; à ce stade, aucune authentification n'existe encore.
 */
@RestController
@RequestMapping("/api/agents")
public class AgentController {

	private final AgentRepository agents;

	public AgentController(AgentRepository agents) {
		this.agents = agents;
	}

	@GetMapping
	public List<AgentResume> lister() {
		return this.agents.findByActifTrueOrderByNomCompletAsc().stream().map(AgentResume::de).toList();
	}
}
