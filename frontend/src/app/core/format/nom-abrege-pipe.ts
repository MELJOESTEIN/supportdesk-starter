import { Pipe, PipeTransform } from '@angular/core';

/**
 * « Bob Lefevre » → « Bob L. »
 *
 * Les colonnes denses de la maquette abrègent le patronyme : sans ça, la colonne
 * « dernière activité » passe sur deux lignes et la ligne de tableau perd sa hauteur de
 * 36 px. Le nom complet reste disponible en infobulle.
 */
@Pipe({ name: 'nomAbrege' })
export class NomAbregePipe implements PipeTransform {
  transform(valeur: string | null | undefined): string {
    if (!valeur) {
      return '';
    }
    const mots = valeur.trim().split(/\s+/);
    if (mots.length < 2) {
      return valeur;
    }
    return `${mots[0]} ${mots.at(-1)!.charAt(0).toUpperCase()}.`;
  }
}
