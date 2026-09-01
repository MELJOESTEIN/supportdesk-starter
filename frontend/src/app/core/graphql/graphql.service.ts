import { HttpClient } from '@angular/common/http';
import { Injectable, Signal, inject } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { Observable, map } from 'rxjs';

/** Réponse GraphQL : les erreurs voyagent dans le corps, avec un HTTP 200. */
export interface ReponseGraphQl<T> {
  data?: T;
  errors?: { message: string; path?: (string | number)[] }[];
}

/**
 * Client GraphQL minimal.
 *
 * <p>Pas d'Apollo : le back-office fait cinq requêtes, toutes connues à l'avance. Une
 * bibliothèque de cache normalisé apporterait ici plus de concepts que de service — et
 * `httpResource()` donne déjà les signaux de chargement et d'erreur.
 *
 * <p>Le jeton est ajouté par l'interceptor OIDC : `/graphql` figure dans les `secureRoutes`.
 */
@Injectable({ providedIn: 'root' })
export class GraphQlService {
  private readonly http = inject(HttpClient);

  /**
   * Requête réactive.
   *
   * <p>Relance l'appel quand les variables changent, et **remonte les erreurs GraphQL comme
   * des erreurs** : sans ça, un HTTP 200 contenant `{"errors":[…]}` passerait pour un
   * succès et l'écran afficherait un état vide sans rien dire.
   */
  interroger<T>(document: string, variables: Signal<Record<string, unknown>>) {
    return httpResource<T>(
      () => ({
        url: '/graphql',
        method: 'POST',
        body: { query: document, variables: variables() },
      }),
      {
        parse: (brut) => {
          const reponse = brut as ReponseGraphQl<T>;
          if (reponse.errors?.length) {
            throw new Error(reponse.errors.map((e) => e.message).join(' · '));
          }
          return reponse.data as T;
        },
      },
    );
  }

  /** Mutation ponctuelle. */
  muter<T>(document: string, variables: Record<string, unknown> = {}): Observable<T> {
    return this.http
      .post<ReponseGraphQl<T>>('/graphql', { query: document, variables })
      .pipe(
        map((reponse) => {
          if (reponse.errors?.length) {
            throw new Error(reponse.errors.map((e) => e.message).join(' · '));
          }
          return reponse.data as T;
        }),
      );
  }
}
