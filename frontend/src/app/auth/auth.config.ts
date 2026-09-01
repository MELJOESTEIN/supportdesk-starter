import { LogLevel, PassedInitialConfig } from 'angular-auth-oidc-client';

import { environment } from '../../environments/environment';

/**
 * Flux « code d'autorisation avec PKCE », le seul recommandé pour une application qui
 * s'exécute dans un navigateur.
 *
 * <p>Le client est <b>public</b> : il n'a pas de secret, et il ne pourrait pas en garder un.
 * PKCE remplace le secret par une preuve à usage unique — le `code_verifier` — que seul le
 * navigateur qui a initié la redirection possède. `S256` est la méthode de hachage ;
 * `plain` n'apporte rien et ne doit jamais être utilisée.
 */
export const configurationAuth: PassedInitialConfig = {
  config: {
    authority: environment.keycloak.autorite,
    redirectUrl: `${window.location.origin}/connexion/retour`,
    postLogoutRedirectUri: `${window.location.origin}/deconnexion`,
    clientId: environment.keycloak.clientId,
    scope: 'openid profile email',
    responseType: 'code',
    // Rotation du jeton de rafraîchissement : chaque usage en invalide le précédent.
    silentRenew: true,
    useRefreshToken: true,
    renewTimeBeforeTokenExpiresInSeconds: 30,
    // Le jeton d'accès vaut 5 minutes (accessTokenLifespan du realm) : sans renouvellement
    // silencieux, l'utilisateur serait déconnecté au milieu d'une saisie.
    ignoreNonceAfterRefresh: true,
    // Le jeton ne part QUE vers notre API. Une liste blanche, jamais un joker : un
    // interceptor qui ajoute l'en-tête partout finit par l'envoyer à un tiers.
    secureRoutes: [environment.api, '/graphql'],
    logLevel: environment.production ? LogLevel.Error : LogLevel.Warn,
  },
};
