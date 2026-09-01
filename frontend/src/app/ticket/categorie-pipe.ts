import { Pipe, PipeTransform } from '@angular/core';

import { CategorieTicket } from './ticket.model';

const LIBELLES: Record<CategorieTicket, string> = {
  FACTURATION: 'Facturation',
  ACCES: 'Accès et connexion',
  ANOMALIE: 'Anomalie technique',
  EVOLUTION: "Demande d'évolution",
  AUTRE: 'Autre',
};

/**
 * Enum technique → libellé lisible.
 *
 * Le DTO transporte `FACTURATION` ; l'écran affiche « Facturation ». La traduction vit
 * ici, pas dans le backend : c'est de la présentation, et le jour où l'API sert une autre
 * langue, seul ce fichier bouge.
 */
@Pipe({ name: 'categorie' })
export class CategoriePipe implements PipeTransform {
  transform(valeur: CategorieTicket | null | undefined): string {
    return valeur ? (LIBELLES[valeur] ?? valeur) : '—';
  }
}
