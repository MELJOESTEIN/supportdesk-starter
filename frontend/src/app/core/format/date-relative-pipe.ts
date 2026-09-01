import { Pipe, PipeTransform } from '@angular/core';

const MINUTE = 60_000;
const HEURE = 60 * MINUTE;
const JOUR = 24 * HEURE;

/**
 * « il y a 14 min », « il y a 2 h », « hier, 17:42 », « 14 août, 09:15 ».
 *
 * Le format de la maquette. La date absolue reste disponible en infobulle via `title` :
 * un utilisateur qui ouvre un litige a besoin de l'heure exacte.
 */
@Pipe({ name: 'dateRelative' })
export class DateRelativePipe implements PipeTransform {
  transform(valeur: string | null | undefined, maintenant = Date.now()): string {
    if (!valeur) {
      return '—';
    }

    const date = new Date(valeur);
    const ecart = maintenant - date.getTime();

    if (ecart < MINUTE) {
      return "à l'instant";
    }
    if (ecart < HEURE) {
      return `il y a ${Math.floor(ecart / MINUTE)} min`;
    }
    if (ecart < JOUR) {
      return `il y a ${Math.floor(ecart / HEURE)} h`;
    }
    if (ecart < 2 * JOUR) {
      return `hier, ${heure(date)}`;
    }
    if (ecart < 7 * JOUR) {
      return `il y a ${Math.floor(ecart / JOUR)} j`;
    }

    return `${date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })}, ${heure(date)}`;
  }
}

function heure(date: Date): string {
  return date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
}
