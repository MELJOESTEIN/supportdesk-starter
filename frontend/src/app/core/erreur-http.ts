import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { Session } from '../auth/session';

/**
 * Traduction des erreurs HTTP en navigation.
 *
 * <p>Trois cas, trois écrans distincts, et la distinction compte :
 * <ul>
 *   <li><b>401</b> — la session a expiré. Écran « Session terminée », fond sombre.</li>
 *   <li><b>403</b> — la session est valide, mais la ressource appartient à quelqu'un
 *       d'autre. Écran « Ce ticket appartient à un autre compte », dans la coque de
 *       l'application connectée.</li>
 *   <li>le reste — remonte au composant, qui affiche son propre état d'erreur.</li>
 * </ul>
 *
 * <p>Confondre les deux premiers conduit à déconnecter quelqu'un qui a simplement cliqué
 * sur un mauvais lien.
 */
export const interceptorErreurs: HttpInterceptorFn = (requete, suivant) => {
  const router = inject(Router);
  const session = inject(Session);

  return suivant(requete).pipe(
    catchError((erreur: HttpErrorResponse) => {
      if (erreur.status === 401) {
        // `terminerLocalement()` et non `deconnecter()` : voir Session#terminerLocalement.
        // Un 401 signifie que notre jeton est refusé, pas qu'il faut fermer la session
        // Keycloak — et si elle est déjà fermée, la fermer une seconde fois échoue.
        session.terminerLocalement();
        void router.navigate(['/deconnexion']);
      }
      return throwError(() => erreur);
    }),
  );
};
