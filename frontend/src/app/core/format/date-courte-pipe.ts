import { Pipe, PipeTransform } from '@angular/core';

/** « 24 août 2026 » — colonne « créé le ». */
@Pipe({ name: 'dateCourte' })
export class DateCourtePipe implements PipeTransform {
  transform(valeur: string | null | undefined): string {
    if (!valeur) {
      return '—';
    }
    return new Date(valeur).toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  }
}
