# `verif/` — la démonstration, rejouable

Collection **REST Client** (extension `humao.rest-client`, déjà recommandée dans
`.vscode/extensions.json`). Ouvre un fichier, clique sur `Send Request` au-dessus d'une
requête.

## Pourquoi ici et pas dans Swagger UI

Swagger sert à explorer une API qu'on ne connaît pas. Il ne sert pas à **démontrer** :
son bouton « Authorize » range le jeton dans un coin de l'interface et l'envoie tout seul.
Or c'est précisément le transport du jeton qu'il faut voir — qui l'obtient, ce qu'il
contient, ce qui se passe quand on prend celui du voisin.

Ici, chaque requête montre son en-tête `Authorization`. Le fichier `40-bola.http` fait tenir
toute la leçon du J2 en une page.

## Ordre de lecture en séance

## Trois guides pas à pas

Avant les collections `.http`, trois parcours guidés, avec la sortie attendue à chaque étape :

| Guide | Ce qu'on y fait |
|---|---|
| **[`GUIDE-PRATIQUE.md`](GUIDE-PRATIQUE.md)** | éprouver REST, GraphQL et SOAP — dix étapes |
| **[`GUIDE-SOAP.md`](GUIDE-SOAP.md)** | SOAP de bout en bout : contrat, faults, façade, panne |
| **[`GUIDE-SECURITE.md`](GUIDE-SECURITE.md)** | **attaquer son propre projet** — six manches, du jeton au `429` |

## Le bonus

**[`KIT-IA-CLAUDE-CODE.pdf`](KIT-IA-CLAUDE-CODE.pdf)** — cinq pages sur la configuration d'un
projet pour travailler avec Claude Code : `CLAUDE.md`, règles conditionnelles, skills,
subagents, hooks, et surtout ce qu'un agent ne trouvera jamais tout seul.

## Les collections

| Fichier | Ce qu'il montre |
|---|---|
| `00-tokens.http` | où naît un jeton, et ce qu'il contient |
| `10-parcours-client.http` | le parcours nominal d'alice |
| `20-parcours-agent.http` | ce que bob voit de plus, et pourquoi |
| **`40-bola.http`** | **la tentative qui doit échouer** |
| `50-crm.http` | un fault SOAP traduit en `ProblemDetail` |
| `51-graphql.http` | une requête, plusieurs ressources — et ses garde-fous |
| `pkce.sh` | le flux du navigateur, et ce que PKCE protège |

## Prérequis

```bash
docker compose up -d          # les trois dépendances
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Les jetons expirent au bout de **cinq minutes** (`accessTokenLifespan` du realm). Rejoue
`00-tokens.http` quand une requête répond 401 avec `Jwt expired`.
