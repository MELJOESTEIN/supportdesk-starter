import { Pipe, PipeTransform } from '@angular/core';

/** « 24 août 2026 à 09:12 » — en-tête de détail et infobulles. */
@Pipe({ name: 'dateLongue' })
export class DateLonguePipe implements PipeTransform {
  transform(valeur: string | null | undefined): string {
    if (!valeur) {
      return '—';
    }
    const date = new Date(valeur);
    const jour = date.toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
    const heure = date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    return `${jour} à ${heure}`;
  }
}
