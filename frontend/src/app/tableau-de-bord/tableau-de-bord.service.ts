import { Injectable, inject, signal } from '@angular/core';

import { GraphQlService } from '../core/graphql/graphql.service';
import { REQUETE_TABLEAU_DE_BORD } from './tableau-de-bord.graphql';
import { TableauDeBord } from './tableau-de-bord.model';

/**
 * Tableau de bord, servi par GraphQL.
 *
 * <p>Le serveur refuse la requête à un CLIENT — la garde de route ne fait que lui éviter
 * un écran vide.
 */
@Injectable({ providedIn: 'root' })
export class TableauDeBordService {
  private readonly graphql = inject(GraphQlService);

  charger(jours = 14) {
    const variables = signal({ jours });
    return this.graphql.interroger<{ tableauDeBord: TableauDeBord }>(
      REQUETE_TABLEAU_DE_BORD,
      variables,
    );
  }
}
