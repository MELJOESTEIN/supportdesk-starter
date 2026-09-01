# `verif/` — éprouver l'API à la main

Collection **REST Client** (extension `humao.rest-client`, déjà recommandée dans
`.vscode/extensions.json`). Ouvre un fichier, clique sur `Send Request` au-dessus d'une requête.

## Pourquoi ici et pas dans Swagger UI

Swagger sert à explorer une API qu'on ne connaît pas. Il ne sert pas à **démontrer** : son bouton
« Authorize » range le jeton dans un coin de l'interface et l'envoie tout seul. Or c'est
précisément le transport du jeton qu'il faut voir — qui l'obtient, ce qu'il contient, ce qui se
passe quand on prend celui du voisin.

Ici, chaque requête montre son en-tête `Authorization`.

## Ce que tu as aujourd'hui

| Fichier | Ce qu'il montre |
|---|---|
| `jeton.sh` | obtenir un jeton en ligne de commande — `./jeton.sh alice --claims` l'ouvre |
| `00-tokens.http` | où naît un jeton, et ce qu'il contient |
| `10-parcours-client.http` | le parcours nominal d'un client |
| `pkce.sh` | le flux du navigateur, et ce que PKCE protège |

D'autres fichiers arrivent au fil de la semaine, quand la manipulation qu'ils portent a un sens.

## Prérequis

```bash
docker compose up -d          # les trois dépendances
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Les jetons expirent au bout de **cinq minutes** (`accessTokenLifespan` du realm). Rejoue
`00-tokens.http` quand une requête répond 401 avec `Jwt expired`.

## Les quatre comptes de test

Mot de passe : `password`.

| Login | Rôle | Référence CRM |
|---|---|---|
| `alice` | CLIENT | CLI-0001 |
| `david` | CLIENT | CLI-0002 |
| `bob` | AGENT | — |
| `carol` | ADMIN + AGENT | — |
