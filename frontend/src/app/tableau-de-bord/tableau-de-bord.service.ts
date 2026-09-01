import { Injectable, resource } from '@angular/core';

import { TABLEAU_DE_BORD } from './tableau-de-bord.fixtures';
import { TableauDeBord } from './tableau-de-bord.model';

/** Lot 2 : fixture. Lot 3 : `GET /api/tableau-de-bord`. Lot 5 : requête GraphQL. */
@Injectable({ providedIn: 'root' })
export class TableauDeBordService {
  charger() {
    return resource<TableauDeBord, unknown>({
      params: () => ({}),
      loader: async () => TABLEAU_DE_BORD,
    });
  }
}
