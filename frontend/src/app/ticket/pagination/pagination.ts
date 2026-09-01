import { Component, computed, input, output } from '@angular/core';

import { PageReponse } from '../../core/page.model';

/** Pied de tableau : « 1–6 sur 18 » et les numéros de page. */
@Component({
  selector: 'sd-pagination',
  templateUrl: './pagination.html',
  styleUrl: './pagination.scss',
})
export class Pagination {
  readonly page = input.required<PageReponse<unknown>>();
  readonly taillesDisponibles = input<number[] | null>(null);

  readonly changerPage = output<number>();
  readonly changerTaille = output<number>();

  protected readonly intervalle = computed(() => {
    const p = this.page();
    if (p.total === 0) {
      return '0 résultat';
    }
    const debut = p.page * p.taille + 1;
    const fin = Math.min((p.page + 1) * p.taille, p.total);
    return `${debut}–${fin} sur ${p.total}`;
  });

  protected readonly numeros = computed(() => {
    const total = this.page().totalPages;
    return Array.from({ length: Math.min(total, 3) }, (_, i) => i);
  });

  protected readonly aDesPagesMasquees = computed(() => this.page().totalPages > 3);

  protected surTaille(valeur: string): void {
    this.changerTaille.emit(Number(valeur));
  }
}
