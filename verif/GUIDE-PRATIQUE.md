# Éprouver les trois API, à la main

Guide pas à pas. Chaque étape a une **sortie attendue** : si tu obtiens autre chose, ne passe pas
à la suivante — tu viens de trouver quelque chose.

Toutes les sorties de ce guide ont été obtenues le **2 septembre 2026** sur une base fraîche.

---

## 0 · Démarrer, et vérifier que tu démarres bien

```bash
docker compose up -d
docker compose ps          # les 3 doivent être « healthy »
```

Puis, dans deux terminaux séparés :

```bash
cd backend  && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
cd frontend && npm start
```

**Attendu :**

```
sd-postgres     Up (healthy)
sd-keycloak     Up (healthy)
sd-legacy-crm   Up (healthy)
```

Le profil `dev` allume les logs SQL. Tu en auras besoin à l'étape 6.

---

## 1 · Le jeton, d'abord

Sans jeton, tout répond `401`. **Toutes les commandes de ce guide se lancent depuis la racine du
projet**, pas depuis `verif/`.

```bash
cd verif && ./jeton.sh alice --claims && cd ..
```

**Attendu :**

```
preferred_username : alice
crm_client_ref     : CLI-0001
realm_access       : ['CLIENT']
aud                : supportdesk-api
```

Puis range les trois jetons dont tu auras besoin :

```bash
ALICE=$(cd verif && ./jeton.sh alice)
DAVID=$(cd verif && ./jeton.sh david)
BOB=$(cd verif && ./jeton.sh bob)
echo "alice=${#ALICE} david=${#DAVID} bob=${#BOB} caractères"
```

**Attendu :** trois nombres autour de 1200. **Si tu vois `0`**, le `cd verif` a échoué — tu n'es
pas à la racine du projet. C'est l'erreur la plus fréquente de ce guide.

> Les jetons expirent au bout de **cinq minutes**. Quand une requête qui marchait répond soudain
> `401`, relance ces trois lignes avant de chercher plus loin.

---

## 2 · REST — les dix endpoints

Le backend expose **10 endpoints** répartis sur 4 contrôleurs.

### Lecture

```bash
curl -s -o /dev/null -w "tickets            %{http_code}\n" -H "Authorization: Bearer $BOB" localhost:8080/api/tickets
curl -s -o /dev/null -w "un ticket          %{http_code}\n" -H "Authorization: Bearer $BOB" localhost:8080/api/tickets/1
curl -s -o /dev/null -w "clients de la file %{http_code}\n" -H "Authorization: Bearer $BOB" localhost:8080/api/tickets/clients
curl -s -o /dev/null -w "tableau de bord    %{http_code}\n" -H "Authorization: Bearer $BOB" localhost:8080/api/tableau-de-bord
curl -s -o /dev/null -w "agents             %{http_code}\n" -H "Authorization: Bearer $BOB" localhost:8080/api/agents
curl -s -o /dev/null -w "une fiche client   %{http_code}\n" -H "Authorization: Bearer $BOB" localhost:8080/api/clients/CLI-0001
curl -s -o /dev/null -w "recherche client   %{http_code}\n" -H "Authorization: Bearer $BOB" "localhost:8080/api/clients?recherche=atelier"
```

**Attendu : sept fois `200`.**

### Écriture

```bash
curl -s -o /dev/null -w "création      %{http_code}\n" -X POST -H "Authorization: Bearer $ALICE" -H "Content-Type: application/json" \
  -d '{"sujet":"Mon premier ticket","description":"Écrit à la main","categorie":"ANOMALIE"}' localhost:8080/api/tickets

curl -s -o /dev/null -w "commentaire   %{http_code}\n" -X POST -H "Authorization: Bearer $ALICE" -H "Content-Type: application/json" \
  -d '{"contenu":"un commentaire","visibilite":"PUBLIC"}' localhost:8080/api/tickets/1/commentaires

curl -s -o /dev/null -w "modification  %{http_code}\n" -X PATCH -H "Authorization: Bearer $BOB" -H "Content-Type: application/json" \
  -d '{"priorite":"HAUTE"}' localhost:8080/api/tickets/1
```

**Attendu : `201`, `200`, `200`.** Le `201` est normal — c'est le code d'une création.

> **Piège de catégorie.** Les seules valeurs acceptées sont `FACTURATION`, `ACCES`, `ANOMALIE`,
> `EVOLUTION`, `AUTRE`. Écris `TECHNIQUE` et tu obtiens un `400` — l'enum n'existe pas.

### Ce que tu dois avoir compris

L'**URL** désigne la ressource, le **verbe** désigne l'action, le **code de statut** porte le
résultat. Rien n'est inventé : tout ce vocabulaire vient de HTTP.

---

## 3 · Trois familles d'erreurs, sur le même endpoint

C'est l'exercice le plus instructif de la matinée. Les trois échouent, mais pas de la même façon.

```bash
echo "--- 1 · un champ obligatoire vide ---"
curl -s -X POST -H "Authorization: Bearer $ALICE" -H "Content-Type: application/json" \
  -d '{"sujet":"","description":"x","categorie":"ANOMALIE"}' localhost:8080/api/tickets

echo; echo "--- 2 · une valeur d'enum qui n'existe pas ---"
curl -s -X POST -H "Authorization: Bearer $ALICE" -H "Content-Type: application/json" \
  -d '{"sujet":"x","description":"y","categorie":"TECHNIQUE"}' localhost:8080/api/tickets

echo; echo "--- 3 · un champ que le DTO ne prévoit pas ---"
curl -s -X POST -H "Authorization: Bearer $ALICE" -H "Content-Type: application/json" \
  -d '{"sujet":"x","description":"y","categorie":"ANOMALIE","statut":"FERME"}' localhost:8080/api/tickets
```

**Attendu :**

1. un `ProblemDetail` complet, avec `"champs":{"sujet":"Le sujet est obligatoire"}` — il **nomme
   le champ fautif** ;
2. un `400` au format Spring par défaut (`timestamp`, `error`, `path`) — l'erreur survient à la
   désérialisation, **avant** que la validation ne s'exécute ;
3. **un `201`** : le ticket est créé, et `statut` est **ignoré**.

Le troisième cas est le plus important. Vérifie le statut du ticket que tu viens de créer : il est
`OUVERT`, pas `FERME`. **Le DTO d'entrée ne contient pas ce champ, donc le client ne peut pas le
fixer.** C'est la protection contre l'*affectation en masse* — et elle vient de la forme du
`record`, pas d'un contrôle écrit à la main.

---

## 4 · GraphQL — la même donnée, par l'autre chemin

```bash
gql() { curl -s -X POST -H "Authorization: Bearer $BOB" -H "Content-Type: application/json" -d "{\"query\":\"$1\"}" localhost:8080/graphql; }
```

### Les trois requêtes

```bash
gql '{ tickets(taille:3){ total contenu { reference statut } } }'
gql '{ ticket(id:1){ reference sujet statut } }'
gql '{ tableauDeBord(jours:14){ totalPeriode indicateurs { cle valeur } } }'
```

**Attendu :** trois réponses avec une clé `data`, aucune clé `errors`.

### Les quatre mutations

```bash
gql 'mutation { changerStatut(id:1, statut:EN_COURS){ reference statut } }'
gql 'mutation { changerPriorite(id:1, priorite:HAUTE){ reference priorite } }'
gql 'mutation { assigner(id:1, agent:\"bob\"){ reference assigneA { nomComplet } } }'
gql 'mutation { ajouterCommentaire(id:1, contenu:\"test\", visibilite:PUBLIC){ reference } }'
```

**Attendu :** quatre `data`. Remarque que tu **choisis les champs de retour**, exactement comme
pour une lecture — c'est propre à GraphQL.

### Le contraste qui explique tout

Compare ces deux commandes. Même question, deux protocoles :

```bash
# REST : le serveur décide de ce qu'il renvoie
curl -s -H "Authorization: Bearer $BOB" localhost:8080/api/tickets/1 | head -c 200

# GraphQL : tu décides
gql '{ ticket(id:1){ reference } }'
```

La réponse REST fait plus de mille octets. La réponse GraphQL en fait moins de cinquante — parce
que tu n'as demandé qu'un champ.

---

## 5 · Les deux URL de GraphQL, à ne pas confondre

```bash
curl -s -o /dev/null -w "POST /graphql   %{http_code}\n" -X POST -H "Authorization: Bearer $BOB" \
  -H "Content-Type: application/json" -d '{"query":"{ tickets{ total } }"}' localhost:8080/graphql
curl -s -o /dev/null -w "GET  /graphql   %{http_code}\n" -H "Authorization: Bearer $BOB" localhost:8080/graphql
curl -s -o /dev/null -w "GET  /graphiql  %{http_code}\n" localhost:8080/graphiql
```

**Attendu : `200`, `405`, `307`.**

| | |
|---|---|
| `/graphql` | **l'API**. N'accepte que `POST`. Un `GET` renvoie `405` |
| `/graphiql` | **l'explorateur web**. Un outil de développement, coupé en production |

Ouvre <http://localhost:8080/graphiql> dans ton navigateur : la complétion connaît tous les champs
du schéma. C'est l'introspection — un service REST ne peut pas faire ça sans documentation
séparée.

---

## 6 · Le N+1, et le batching qui le corrige

Le moment le plus important de l'après-midi.

```bash
gql '{ tickets(taille:25){ total contenu { reference client { raisonSociale } } } }' \
  | python3 -c "import sys,json; d=json.load(sys.stdin)['data']['tickets']; \
    print(d['total'],'tickets ·', len({t['client']['raisonSociale'] for t in d['contenu'] if t.get('client')}),'clients distincts')"
```

**Attendu : `7 clients distincts`.** Le nombre de tickets est de 25, plus ceux que tu as créés à
l'étape 2 — c'est le **7** qui compte ici.

Maintenant regarde les logs du backend. **Compte les appels au CRM.** Il y en a **7**, pas 25 —
un par référence *distincte*.

Le CRM répond en **400 ms**. Fais le calcul :

```
sans regroupement   25 × 400 ms  ≈  10 secondes
avec @BatchMapping   7 × 400 ms  ≈  2,8 secondes
```

### Pourquoi c'est pire qu'en REST

En REST, le N+1 est **dans ton code** : tu peux le lire. En GraphQL, il naît de **la forme de la
requête du client**. Le même resolver est innocent ou catastrophique selon ce qu'on lui demande —
et c'est le client qui décide.

Ouvre `backend/src/main/java/com/supportdesk/graphql/TicketGraphQlController.java` et cherche
`@BatchMapping`. C'est là que ça se joue.

---

## 7 · Les erreurs GraphQL ne sont pas les erreurs REST

```bash
gql '{ ticket(id:999999){ reference } }'
```

**Attendu — et lis-le en entier :**

```json
{
  "errors": [{
    "message": "Aucun ticket avec l'identifiant 999999",
    "path": ["ticket"],
    "extensions": { "code": "RESSOURCE_INTROUVABLE", "classification": "NOT_FOUND" }
  }],
  "data": { "ticket": null }
}
```

Vérifie le code HTTP :

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X POST -H "Authorization: Bearer $BOB" \
  -H "Content-Type: application/json" -d '{"query":"{ ticket(id:999999){ reference } }"}' localhost:8080/graphql
```

**Attendu : `200`.**

> **La différence à retenir.** En REST, l'erreur est dans le **code de statut** — `404`, `403`,
> `400`. En GraphQL, la requête a réussi (`200`) et c'est un **champ** qui a échoué : l'erreur
> voyage dans le corps, avec un `path` qui dit lequel.
>
> Ce n'est pas un détail : ça permet à un écran de se rendre **partiellement** au lieu de tout
> perdre. Demande dix champs, un seul échoue, tu affiches les neuf autres.

---

## 8 · La sécurité ne se met pas sur l'URL

En REST, chaque endpoint a son chemin — on peut donc protéger par chemin. En GraphQL, **toutes les
opérations partagent une URL et une méthode**.

```bash
curl -s -o /dev/null -w "agent      %{http_code}\n" -X POST -H "Authorization: Bearer $BOB" \
  -H "Content-Type: application/json" -d '{"query":"{ tickets{ total } }"}' localhost:8080/graphql
curl -s -o /dev/null -w "client     %{http_code}\n" -X POST -H "Authorization: Bearer $ALICE" \
  -H "Content-Type: application/json" -d '{"query":"{ tickets{ total } }"}' localhost:8080/graphql
curl -s -o /dev/null -w "sans jeton %{http_code}\n" -X POST \
  -H "Content-Type: application/json" -d '{"query":"{ tickets{ total } }"}' localhost:8080/graphql
```

**Attendu : `200`, `403`, `401`.**

`/graphql` ne distingue pas « lis mon profil » de « supprime tout ». L'autorisation doit donc vivre
**sur chaque méthode**, pas sur le chemin. Ouvre le contrôleur GraphQL et compte les
`@PreAuthorize` : il y en a un par opération.

---

## 9 · SOAP — parler au système qu'on ne peut pas changer

```bash
curl -s -o /dev/null -w "le contrat WSDL  %{http_code}\n" localhost:8082/services/clients.wsdl
curl -s -o /dev/null -w "l'URL nue        %{http_code}\n" localhost:8082/services
```

**Attendu : `200` puis `405`.** `/services` est une **adresse d'envoi**, pas une page : elle
n'accepte que `POST`. Beaucoup d'étudiants croient le service cassé à cause de ça.

L'appel direct, en XML :

```bash
curl -s -X POST http://localhost:8082/services \
  -H 'Content-Type: text/xml;charset=UTF-8' \
  -w '\n[%{time_total}s]\n' \
  -d '<?xml version="1.0"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body>
<GetClientRequest xmlns="http://legacy.acme.fr/crm"><clientRef>CLI-0001</clientRef></GetClientRequest>
</soap:Body></soap:Envelope>'
```

**Attendu :** du XML contenant `Transports Nord`, en **~0,4 seconde**. Cette lenteur est
volontaire — c'est celle d'un vrai système existant.

### Les faults, traduits par la façade

```bash
curl -s -o /dev/null -w "référence inconnue   %{http_code}\n" -H "Authorization: Bearer $BOB" localhost:8080/api/clients/CLI-9999
curl -s -o /dev/null -w "recherche sans motif %{http_code}\n" -H "Authorization: Bearer $BOB" "localhost:8080/api/clients?recherche="
```

**Attendu : `404` et `400`.** Le CRM a répondu par des *faults* SOAP — `CLIENT_INCONNU` et
`CRITERE_OBLIGATOIRE`. Ton backend les a traduits en codes HTTP.

> **Un fault est une réponse prévue par le contrat, pas une panne.** Le laisser remonter en `500`
> ferait passer une règle métier pour un incident.

---

## 10 · Quand le legacy meurt

> **Avant de commencer, vide le cache.** À l'étape 6, ta requête de batching a chargé les sept
> fiches clients, et elles y sont restées. Sans ce redémarrage, tout répondra `200` et tu ne verras
> rien. Le cache vit en mémoire : arrêter le backend suffit.
>
> ```bash
> # dans le terminal du backend : Ctrl+C, puis
> cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
> ```

```bash
docker compose stop legacy-crm
```

Puis :

```bash
curl -s -o /dev/null -w "la liste des tickets  %{http_code}\n" -H "Authorization: Bearer $BOB" localhost:8080/api/tickets
curl -s -o /dev/null -w "une fiche jamais lue  %{http_code}\n" -H "Authorization: Bearer $BOB" localhost:8080/api/clients/CLI-0006
```

**Attendu : `200` puis `503`.**

La liste **continue de s'afficher**, avec les références brutes (`CLI-0001`) au lieu des raisons
sociales. Seule la fiche client, qui ne peut pas exister sans le CRM, échoue.

> Un référentiel injoignable ne doit pas emporter ton back-office. **Un confort d'affichage ne
> devient jamais une dépendance dure.**

### La surprise à observer

Le CRM est toujours coupé. Redemande la fiche que tu viens de voir échouer :

```bash
curl -s -o /dev/null -w "premier appel   %{http_code}\n" -H "Authorization: Bearer $BOB" localhost:8080/api/clients/CLI-0001
docker compose start legacy-crm && sleep 25
curl -s -o /dev/null -w "CRM redémarré   %{http_code}\n" -H "Authorization: Bearer $BOB" localhost:8080/api/clients/CLI-0001
docker compose stop legacy-crm && sleep 2
curl -s -o /dev/null -w "CRM re-coupé    %{http_code}   ← le cache\n" -H "Authorization: Bearer $BOB" localhost:8080/api/clients/CLI-0001
```

**Attendu : `503`, `200`, puis `200`** — le dernier alors que le CRM est mort. C'est le **cache** :
une fois la fiche chargée, elle est servie sans toucher au CRM.

Trois questions à te poser, et elles n'ont pas de réponse évidente :

- Est-ce une bonne nouvelle ?
- Combien de temps ça peut durer ?
- L'utilisateur sait-il qu'il regarde une donnée peut-être périmée ?

```bash
docker compose start legacy-crm      # ne l'oublie pas
```

---

## Ce que tu dois savoir dire à la fin

- Pourquoi `POST /api/tickets` avec `"statut":"FERME"` crée quand même un ticket `OUVERT`.
- Où se trouve le `@BatchMapping`, et ce qui se passerait sans lui.
- Pourquoi une erreur GraphQL arrive avec un code HTTP `200`.
- Pourquoi l'autorisation GraphQL ne peut pas se poser sur l'URL.
- Ce qu'est un *fault*, et pourquoi ce n'est pas une panne.

Si tu ne sais pas répondre à l'une d'elles, reprends l'étape correspondante — le guide est fait
pour être rejoué.
