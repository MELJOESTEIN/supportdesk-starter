# Jour 2 — les deux prompts

Deux blocs à coller dans Claude Code, **dans l'ordre**, à des moments différents de la journée.
Le second ne se colle qu'après la démonstration de l'après-midi : le lire ce matin gâcherait
l'exercice qui compte le plus de la semaine.

Session lancée **à la racine du projet**, jamais dans `backend/` ou `frontend/`. Vérifie avec
`/context` que `CLAUDE.md` est chargé.

---

## Bloc 1 — le matin

```
/tuteur-spring-boot
```

Puis :

> Aujourd'hui j'ajoute l'identité à SupportDesk. Tu es en mode tuteur : tu ne codes pas à ma
> place, tu me fais coder. Lis `CLAUDE.md` avant de me répondre.
>
> **Où j'en suis.** La chaîne tourne de bout en bout, mais elle est grande ouverte :
> `GET /api/tickets` répond `200` sans le moindre jeton. Keycloak tourne déjà sur `:8081` avec
> le realm `supportdesk`, ses trois rôles et ses quatre comptes — je n'ai pas à le configurer.
>
> **Ce que je veux obtenir à midi.** Travaille par étapes, une seule à la fois, et ne passe à la
> suivante que quand j'ai exécuté la vérification et que je t'ai montré la sortie.
>
> **Étape 1 — le backend refuse un appel anonyme.**
> `curl -s -o /dev/null -w "%{http_code}" localhost:8080/api/tickets` doit renvoyer `401`,
> et `200` avec un jeton valide obtenu par `cd verif && ./jeton.sh alice`.
> Commence par me faire ouvrir un jeton avec `./jeton.sh alice --claims` et par me demander ce
> que je vois dedans. Je veux comprendre ce que le serveur va vérifier avant d'écrire la
> configuration qui le vérifie.
>
> **Étape 2 — un jeton émis pour une autre application est refusé.**
> Il existe dans le même realm un client `intranet-front`, dont les jetons portent
> `aud: intranet-api`. Présenté à mon API, il doit être rejeté. Fais-moi d'abord constater le
> problème, puis écrire la vérification.
>
> **Étape 3 — les rôles de Keycloak deviennent des rôles Spring.**
> Ils arrivent dans le jeton sous `realm_access.roles`, ce qui n'est pas là où Spring les
> cherche. Après cette étape, `alice` (CLIENT) doit recevoir `403` sur
> `/api/tableau-de-bord` et `bob` (AGENT) `200`.
>
> **Étape 4 — le contrôleur sait qui appelle.**
> Je veux pouvoir écrire une méthode de contrôleur qui reçoit l'utilisateur courant en
> paramètre, sans jamais lire le jeton à la main. Explique-moi le mécanisme avant que je
> l'écrive.
>
> **Étape 5 — le front se connecte.**
> Redirection vers Keycloak en PKCE, jeton porté sur les appels à l'API, nom de l'utilisateur
> affiché, déconnexion. Les fixtures en dur du frontend disparaissent : les écrans lisent
> désormais la vraie API.
> Deux pièges sur lesquels je veux que tu m'arrêtes : l'ajout du jeton doit se faire sur une
> **liste blanche** d'URL et jamais partout, et la vérification de session au démarrage est un
> appel réseau — tant qu'elle n'a pas répondu, un clic sur « Se connecter » ne fait rien.
>
> **Comment tu me réponds.** Trois à six phrases. Une étape à la fois. Chaque notion nouvelle
> définie en une phrase la première fois. Tu ne dis jamais « c'est fait » : tu dis « la
> vérification passe » et tu me demandes la sortie de la commande.
>
> Commence par l'étape 1, et par la question que je devrais me poser avant d'écrire quoi que ce
> soit.

---

## Bloc 2 — l'après-midi

> **À ne coller qu'après avoir vu la faille de tes propres yeux.**

À ce moment de la journée, tu as constaté deux choses : qu'un bouton masqué dans Angular
n'empêche pas un `curl` d'atteindre l'endpoint, et qu'avec le jeton d'`alice` tu peux lire un
ticket qui appartient à `david`. Le rôle est bon, l'endpoint est autorisé, et la donnée sort
quand même.

> J'ai reproduit la faille : avec le jeton d'`alice` (CLI-0001), je lis un ticket de `david`
> (CLI-0002). Mon API vérifie qui appelle, mais pas à qui appartient ce qu'elle renvoie.
>
> Toujours en mode tuteur. Ne me donne pas la correction : fais-moi la trouver.
>
> **Première question, avant tout code.** Rouvre `docs/produit.md` avec moi et lis la question 4,
> celle que j'ai remplie hier. Demande-moi si ce que je viens de faire la respecte.
>
> **Ce que je dois obtenir, et que je vérifierai au `curl` :**
>
> - `alice` sur un ticket de `david` → refusé, et **le corps de la réponse ne contient aucune
>   donnée du ticket** : ni le sujet, ni le client. Fais-moi vérifier le corps, pas seulement le
>   code de statut.
> - `alice` sur **son** ticket → `200`. Ce contre-test n'est pas optionnel : sans lui, un endpoint
>   qui ne renverrait plus jamais rien passerait pour corrigé.
> - `bob` (AGENT) sur le même ticket de `david` → `200`. Un agent voit tout, c'est le métier.
> - `alice` qui écrit un commentaire sur le ticket de `david` → refusé aussi. La lecture n'est pas
>   le seul chemin.
>
> **Sur le choix du code de statut**, pose-moi la question plutôt que de trancher : entre `403` et
> `404`, lequel en dit le moins à quelqu'un qui cherche à savoir quels tickets existent ?
>
> **Puis les notes internes.** Un commentaire de visibilité `INTERNE` ne doit jamais atteindre un
> client. Fais-moi vérifier dans la **réponse HTTP**, pas à l'écran : un champ absent de
> l'affichage mais présent dans le JSON a déjà fuité. Demande-moi où le filtrage doit se faire —
> dans la requête vers la base, ou au moment de l'affichage — et pourquoi.
>
> **Enfin, l'endroit.** Quand tout passe, demande-moi de te montrer **la ligne** où l'appartenance
> est vérifiée, et par où passent tous les appelants. Si la vérification est écrite à trois
> endroits, elle sera oubliée à un quatrième.

---

## Ce que tu dois savoir dire en fin de journée, agent fermé

- Où, exactement, ton code vérifie qu'un ticket appartient à celui qui le demande.
- Pourquoi une garde de route Angular ne protège aucune donnée.
- Ce qui empêche un jeton fabriqué à la main d'être accepté.
- Pourquoi `alice` reçoit un refus et `bob` une réponse, sur la même URL.

Si tu ne sais pas montrer la ligne, la journée n'est pas acquise — et c'est le moment de le dire,
pas dans six mois.
