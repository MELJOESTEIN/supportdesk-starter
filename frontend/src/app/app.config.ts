import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // `withComponentInputBinding` lie les paramètres de route aux `input()` des composants :
    // pas d'injection d'ActivatedRoute pour lire un identifiant.
    provideRouter(routes, withComponentInputBinding()),
    // provideHttpClient() est ajouté au lot 4, avec ses interceptors.
  ],
};
