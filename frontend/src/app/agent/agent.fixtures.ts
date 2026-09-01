import { AgentResume } from './agent.model';

/** Alignés sur les utilisateurs du realm : bob (AGENT) et carol (ADMIN + AGENT). */
export const AGENTS: AgentResume[] = [
  { username: 'bob', nomComplet: 'Bob Lefevre', niveau: 'Niveau 2', equipe: 'Facturation' },
  { username: 'carol', nomComplet: 'Carol Nguyen', niveau: 'Niveau 3', equipe: 'Facturation' },
];

export const AGENT_PAR_USERNAME = new Map(AGENTS.map((a) => [a.username, a]));
