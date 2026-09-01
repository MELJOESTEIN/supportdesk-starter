import { HttpParams } from '@angular/common/http';
import { httpResource } from '@angular/common/http';
import { Injectable, Signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { PageReponse } from '../core/page.model';
import {
  FiltreTickets,
  ModificationTicket,
  NouveauCommentaire,
  NouveauTicket,
  OptionClient,
  TicketDetail,
  TicketResume,
} from './ticket.model';

/**
 * Accès aux tickets.
 *
 * <p>Les lectures passent par `httpResource()` : la requête se relance toute seule quand un
 * signal de filtre change, et les états de chargement et d'erreur sont exposés en signaux.
 * Les écritures restent en `HttpClient`, `resource` étant fait pour lire.
 *
 * <p>Aucun paramètre d'identité n'est envoyé. Le périmètre du client vient du jeton, côté
 * serveur : c'était le défaut corrigé au lot 4, et le laisser ici le réintroduirait.
 */
@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly http = inject(HttpClient);

  /** Page de tickets. Le serveur borne le périmètre ; le filtre ne fait que restreindre. */
  pageTickets(filtre: Signal<FiltreTickets>) {
    return httpResource<PageReponse<TicketResume>>(() => ({
      url: `${environment.api}/tickets`,
      params: parametres(filtre()),
    }));
  }

  /**
   * Options du menu « Client » de la file agent.
   *
   * <p>Elles viennent du serveur, jamais d'une liste écrite dans le template : une liste en
   * dur diverge des données au premier client ajouté, et le filtre se met alors à cacher des
   * lignes qu'il affiche par ailleurs. Réservé aux agents côté backend.
   */
  optionsClients() {
    return httpResource<OptionClient[]>(() => `${environment.api}/tickets/clients`);
  }

  /**
   * Détail d'un ticket.
   *
   * <p>Aucun indicateur « pour agent » n'est transmis : le serveur décide, à partir du rôle
   * du jeton, si les notes internes et le journal font partie de la réponse.
   */
  detail(id: Signal<number | null>) {
    return httpResource<TicketDetail>(() => {
      const identifiant = id();
      return identifiant === null ? undefined : `${environment.api}/tickets/${identifiant}`;
    });
  }

  creer(demande: NouveauTicket): Observable<TicketDetail> {
    return this.http.post<TicketDetail>(`${environment.api}/tickets`, demande);
  }

  commenter(id: number, demande: NouveauCommentaire): Observable<TicketDetail> {
    return this.http.post<TicketDetail>(`${environment.api}/tickets/${id}/commentaires`, demande);
  }

  modifier(id: number, demande: ModificationTicket): Observable<TicketDetail> {
    return this.http.patch<TicketDetail>(`${environment.api}/tickets/${id}`, demande);
  }
}

function parametres(filtre: FiltreTickets): HttpParams {
  let params = new HttpParams()
    .set('page', String(filtre.page ?? 0))
    .set('taille', String(filtre.taille ?? 20));

  if (filtre.statuts?.length) {
    params = params.set('statuts', filtre.statuts.join(','));
  }
  if (filtre.crmClientRef) {
    params = params.set('crmClientRef', filtre.crmClientRef);
  }
  if (filtre.assigneA) {
    params = params.set('assigneA', filtre.assigneA);
  }
  if (filtre.recherche?.trim()) {
    params = params.set('recherche', filtre.recherche.trim());
  }
  return params;
}
