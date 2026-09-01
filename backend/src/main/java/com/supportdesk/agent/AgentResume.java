package com.supportdesk.agent;

/** Vue publique d'un agent. Aligné sur `agent.model.ts` du frontend. */
public record AgentResume(String username, String nomComplet, String niveau, String equipe) {

	public static AgentResume de(Agent agent) {
		if (agent == null) {
			return null;
		}
		return new AgentResume(agent.getUsername(), agent.getNomComplet(), agent.getNiveau(),
				agent.getEquipe());
	}
}
