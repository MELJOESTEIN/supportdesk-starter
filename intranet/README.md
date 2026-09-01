# Intranet ACME — la seconde application

Page statique de quelques centaines de lignes, servie par nginx sur le port **4300**. Elle
n'a aucun rapport fonctionnel avec SupportDesk : ni base, ni API, ni système de design en
commun. Un seul point les relie — **le portail d'identité**.

C'est tout l'intérêt.

```bash
docker compose --profile sso up -d intranet     # http://localhost:4300
```

## Ce qu'elle démontre

| | |
|---|---|
| **La session est partagée** | connecté à SupportDesk, on arrive ici sans rien ressaisir |
| **L'autorisation ne l'est pas** | son jeton, présenté à l'API SupportDesk, repart en **401** |
| **Les claims diffèrent** | `crm_client_ref` est absent d'ici : l'intranet n'a pas à connaître la référence CRM d'un client |
| **La déconnexion est unique** | fermer la session ici la ferme aussi pour SupportDesk |

La collection `verif/60-sso.http` rejoue la démonstration requête par requête.

## Pourquoi pas une seconde application Angular

Parce que ça aurait enseigné le contraire de ce qu'on veut montrer.

Tant qu'on ne voit qu'Angular et `angular-auth-oidc-client`, on croit que le SSO est une
affaire de framework. **C'est une propriété du portail d'identité.** Cent lignes de
JavaScript nu participent au même SSO qu'un projet Angular complet — et le flux
`code + PKCE` y est visible en entier, ce qu'aucun écran de SupportDesk ne montre.

Lis `public/app.js` : la génération du `code_verifier`, le haché `S256`, la vérification de
l'`state`, l'échange du code, et le `id_token_hint` de la déconnexion. Une cinquantaine de
lignes utiles pour ce que la bibliothèque fait pour toi dans SupportDesk.

## Ce qu'elle n'est pas

Un exemple d'application de production. Les jetons sont dans `sessionStorage`, il n'y a ni
renouvellement silencieux, ni gestion d'expiration, ni routage. Une vraie application
utiliserait une bibliothèque éprouvée — comme le fait SupportDesk.

**Elle est un support de démonstration, et elle ne se lit que dans ce cadre.**
