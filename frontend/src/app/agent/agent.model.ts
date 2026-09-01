/** Agent de support. Les agents sont en base ; les clients, non (ils viennent du CRM). */
export interface AgentResume {
  username: string;
  nomComplet: string;
  /** Ex. « Niveau 2 » — affiché dans le bloc d'identité de l'en-tête. */
  niveau: string | null;
  equipe: string | null;
}
