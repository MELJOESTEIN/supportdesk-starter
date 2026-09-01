/**
 * Configuration de développement.
 *
 * L'URL de Keycloak est celle que voit le NAVIGATEUR. C'est aussi celle qui figure dans
 * le jeton (`iss`), et donc celle que le backend valide. En production (lot 6), le backend
 * joint Keycloak par un autre nom de service, mais l'issuer ne change pas.
 */
export const environment = {
  production: false,
  api: '/api',
  keycloak: {
    autorite: 'http://localhost:8081/realms/supportdesk',
    clientId: 'supportdesk-front',
  },
};
