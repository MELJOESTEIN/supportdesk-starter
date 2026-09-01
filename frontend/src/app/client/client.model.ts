/**
 * Fiche client du CRM legacy (SOAP).
 *
 * Ces données ne sont **pas** en base : seule `crmClientRef` l'est. Le backend les
 * réexpose en REST après les avoir lues sur le CRM.
 */
export interface ClientCrm {
  clientRef: string;
  raisonSociale: string;
  siret: string;
  contactEmail: string;
  contactTel: string;
  actif: boolean;
}
