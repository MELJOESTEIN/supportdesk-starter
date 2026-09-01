import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { authInterceptor, provideAuth } from 'angular-auth-oidc-client';

import { configurationAuth } from './auth/auth.config';
import { interceptorErreurs } from './core/erreur-http';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideAuth(configurationAuth),
    provideHttpClient(
      // Ordre voulu : le jeton est posé d'abord, puis la traduction des erreurs voit la
      // réponse. `authInterceptor` n'ajoute l'en-tête que sur les `secureRoutes`.
      withInterceptors([authInterceptor(), interceptorErreurs]),
    ),
  ],
};
