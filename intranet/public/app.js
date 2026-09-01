/*
 * Intranet ACME — seconde application du realm `supportdesk`.
 *
 * Le flux « code d'autorisation + PKCE » est écrit ici À LA MAIN, sans bibliothèque. Ce
 * n'est pas de la coquetterie : SupportDesk utilise angular-auth-oidc-client, et tant qu'on
 * ne voit que ça, on croit que le SSO est une affaire de framework. Il n'en est rien — c'est
 * une propriété du portail d'identité. Cent lignes de JavaScript nu participent au même SSO
 * qu'une application Angular complète.
 *
 * Ce que cette page démontre, dans l'ordre :
 *   1. la session est partagée      -> on arrive connecté, sans rien ressaisir ;
 *   2. l'autorisation ne l'est pas  -> ce jeton est refusé par l'API de SupportDesk ;
 *   3. les claims ne sont pas les mêmes -> pas de `crm_client_ref` ici ;
 *   4. la déconnexion est unique    -> fermer ici ferme partout.
 */

const AUTORITE = 'http://localhost:8081/realms/supportdesk';
const CLIENT_ID = 'intranet-front';
const REDIRECTION = window.location.origin + '/';
const API_SUPPORTDESK = 'http://localhost:8080/api/tickets';

const $ = (id) => document.getElementById(id);

/* --- PKCE ---------------------------------------------------------------
 * Le client est public : il n'a pas de secret et ne pourrait pas en garder un. PKCE
 * remplace le secret par une preuve à usage unique. Le `code_verifier` ne quitte jamais
 * ce navigateur ; seul son haché part avec la redirection. Un attaquant qui intercepte
 * le code d'autorisation ne peut rien en faire sans le verifier.
 */
function aleatoire(octets = 32) {
  return base64url(crypto.getRandomValues(new Uint8Array(octets)));
}

function base64url(octets) {
  return btoa(String.fromCharCode(...new Uint8Array(octets)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

async function defi(verifier) {
  const empreinte = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier));
  return base64url(empreinte);
}

/* --- Flux --------------------------------------------------------------- */

async function connecter() {
  const verifier = aleatoire();
  const etat = aleatoire(16);
  sessionStorage.setItem('pkce_verifier', verifier);
  sessionStorage.setItem('pkce_etat', etat);

  const params = new URLSearchParams({
    client_id: CLIENT_ID,
    response_type: 'code',
    redirect_uri: REDIRECTION,
    scope: 'openid profile email',
    state: etat,
    code_challenge: await defi(verifier),
    code_challenge_method: 'S256',
  });
  window.location.assign(`${AUTORITE}/protocol/openid-connect/auth?${params}`);
}

async function echangerLeCode(code, etatRecu) {
  // L'état protège du CSRF : si celui qui revient n'est pas celui qu'on a envoyé, on
  // n'échange rien. Le vérifier est aussi important que PKCE lui-même.
  if (etatRecu !== sessionStorage.getItem('pkce_etat')) {
    throw new Error("L'état renvoyé ne correspond pas à celui qui a été émis.");
  }
  const reponse = await fetch(`${AUTORITE}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: CLIENT_ID,
      code,
      redirect_uri: REDIRECTION,
      code_verifier: sessionStorage.getItem('pkce_verifier'),
    }),
  });
  if (!reponse.ok) {
    throw new Error(`Échange du code refusé (HTTP ${reponse.status}).`);
  }
  const jetons = await reponse.json();
  sessionStorage.setItem('access_token', jetons.access_token);
  sessionStorage.setItem('id_token', jetons.id_token);
  // Conservé pour la revalidation : c'est le seul élément que Keycloak invalide À L'INSTANT
  // où la session est fermée. Le jeton d'accès, lui, reste valide jusqu'à son expiration.
  sessionStorage.setItem('refresh_token', jetons.refresh_token);
  sessionStorage.removeItem('pkce_verifier');
  sessionStorage.removeItem('pkce_etat');
}

/**
 * Demande au portail d'identité si la session existe encore.
 *
 * <p>Le problème à résoudre : quand SupportDesk se déconnecte, la session Keycloak meurt
 * sur-le-champ, mais le jeton d'accès rangé ici reste valide jusqu'à son expiration — cinq
 * minutes. Cette page continuerait donc d'afficher une session fermée.
 *
 * <p>On ne peut pas le savoir en regardant le jeton d'accès : il est signé, daté, et
 * personne ne peut le « dé-émettre ». Le seul élément qui porte le signal est le jeton de
 * rafraîchissement, que Keycloak invalide immédiatement. On tente donc un rafraîchissement :
 * s'il échoue, la session est morte.
 *
 * <p>Déclenché quand l'onglet redevient visible — c'est exactement le geste de l'utilisateur
 * qui revient d'une autre application.
 *
 * @returns true si la session vit toujours
 */
async function sessionToujoursVivante() {
  const rafraichissement = sessionStorage.getItem('refresh_token');
  if (!rafraichissement) {
    return false;
  }
  const reponse = await fetch(`${AUTORITE}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'refresh_token',
      client_id: CLIENT_ID,
      refresh_token: rafraichissement,
    }),
  });
  if (!reponse.ok) {
    return false;
  }
  // La rotation est active : le jeton précédent vient d'être invalidé, il FAUT ranger le
  // nouveau. L'oublier ferait échouer la vérification suivante et déconnecterait à tort.
  const jetons = await reponse.json();
  sessionStorage.setItem('access_token', jetons.access_token);
  sessionStorage.setItem('id_token', jetons.id_token);
  sessionStorage.setItem('refresh_token', jetons.refresh_token);
  return true;
}

function oublierLaSession() {
  sessionStorage.clear();
  $('session').hidden = true;
  $('anonyme').hidden = false;
}

function deconnecter() {
  // `id_token_hint` est obligatoire : sans lui, Keycloak répond par une page d'erreur nue.
  // C'est exactement le défaut qui a bloqué SupportDesk le 30 août.
  const idToken = sessionStorage.getItem('id_token');
  const params = new URLSearchParams({
    post_logout_redirect_uri: REDIRECTION,
    id_token_hint: idToken,
  });
  sessionStorage.clear();
  window.location.assign(`${AUTORITE}/protocol/openid-connect/logout?${params}`);
}

/* --- Affichage ---------------------------------------------------------- */

function claims(jeton) {
  const charge = jeton.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
  return JSON.parse(decodeURIComponent(escape(atob(charge))));
}

const LIGNES = [
  ['sid', 'Session Keycloak', 'Identique à celui du jeton de SupportDesk : une seule session pour les deux applications.'],
  ['aud', 'Audience', "Différente de `supportdesk-api`. C'est ce qui fait échouer l'appel ci-dessous."],
  ['azp', 'Client émetteur', "L'application qui a demandé ce jeton."],
  ['preferred_username', 'Identifiant', ''],
  ['crm_client_ref', 'Référence CRM', "ABSENTE ici, présente dans le jeton de SupportDesk : deux applications d'un même annuaire ne reçoivent pas les mêmes claims."],
];

function afficher(c) {
  $('nom').textContent = c.name || c.preferred_username;
  $('preuve-sso').innerHTML =
    "Vous n'avez rien ressaisi : la session ouverte pour SupportDesk vaut aussi ici. " +
    'C\'est le portail d\'identité qui vous a reconnu, pas cette page.';

  const dl = $('claims');
  dl.innerHTML = '';
  for (const [cle, libelle, aide] of LIGNES) {
    const valeur = c[cle];
    const dt = document.createElement('dt');
    dt.textContent = libelle;
    const dd = document.createElement('dd');
    dd.className = valeur === undefined ? 'claims__absent' : 'claims__valeur';
    dd.innerHTML =
      `<code>${cle}</code> = ` +
      (valeur === undefined ? '<em>absent</em>' : `<strong>${JSON.stringify(valeur)}</strong>`) +
      (aide ? `<span class="claims__aide">${aide}</span>` : '');
    dl.append(dt, dd);
  }
}

async function essayerApiSupportdesk() {
  const sortie = $('resultat-api');
  sortie.hidden = false;
  sortie.textContent = 'Appel en cours…';
  try {
    const r = await fetch(`${API_SUPPORTDESK}?taille=1`, {
      headers: { Authorization: 'Bearer ' + sessionStorage.getItem('access_token') },
    });
    const corps = await r.text();
    sortie.className = r.status === 401 ? 'resultat resultat--attendu' : 'resultat resultat--inattendu';
    sortie.textContent =
      `HTTP ${r.status}\n\n${corps || '(corps vide)'}\n\n` +
      (r.status === 401
        ? "Attendu. L'origine http://localhost:4300 est pourtant AUTORISÉE par CORS : la\n" +
          "requête est bien partie et le serveur a bien répondu. CORS dit qui peut appeler ;\n" +
          "il ne dit jamais qui a le droit. C'est ValidateurAudience qui refuse."
        : "INATTENDU. Un jeton émis pour l'intranet ne doit pas ouvrir l'API de SupportDesk.\n" +
          "Vérifie que l'audience n'a pas été élargie côté realm ou côté backend.");
  } catch (e) {
    sortie.className = 'resultat resultat--inattendu';
    sortie.textContent =
      `La requête n'est même pas partie : ${e.message}\n\n` +
      "C'est un blocage du navigateur (CORS), pas une réponse du serveur — ce n'est donc PAS\n" +
      'la démonstration recherchée. Vérifie que le backend tourne avec le profil dev.';
  }
}

/* --- Démarrage ---------------------------------------------------------- */

(async function demarrer() {
  $('connexion').addEventListener('click', () => connecter());
  $('deconnexion').addEventListener('click', () => deconnecter());
  $('essai-api').addEventListener('click', () => essayerApiSupportdesk());

  // Quand l'onglet redevient visible, on redemande au portail si la session vit. C'est ce
  // qui rend la déconnexion croisée observable en salle : on se déconnecte sur SupportDesk,
  // on revient sur cet onglet, et il a compris.
  document.addEventListener('visibilitychange', async () => {
    if (document.visibilityState !== 'visible' || !sessionStorage.getItem('access_token')) {
      return;
    }
    if (!(await sessionToujoursVivante())) {
      oublierLaSession();
    }
  });

  const url = new URL(window.location.href);
  const code = url.searchParams.get('code');
  const erreur = url.searchParams.get('error');

  try {
    if (erreur) {
      throw new Error(url.searchParams.get('error_description') || erreur);
    }
    if (code) {
      await echangerLeCode(code, url.searchParams.get('state'));
      window.history.replaceState({}, '', REDIRECTION);
    }
    const jeton = sessionStorage.getItem('access_token');
    if (jeton) {
      afficher(claims(jeton));
      $('session').hidden = false;
    } else {
      $('anonyme').hidden = false;
    }
  } catch (e) {
    $('message-erreur').textContent = e.message;
    $('erreur').hidden = false;
  }
})();
