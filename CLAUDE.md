# SupportDesk

Plateforme de gestion de tickets de support. Projet fil rouge de formation full stack.

## Règles pour l'agent

1. **Ne change jamais une version épinglée** dans la section Stack sans me demander d'abord.
2. **Lis la section « Pièges connus » avant d'écrire du code.** Ce projet utilise Spring Boot 4
   et Angular 22 ; beaucoup de ce que tu connais des versions précédentes est faux ici.
3. **N'expose jamais une entité JPA** dans un contrôleur. Toujours un DTO ou une projection.
4. **Une tâche = un commit atomique.** Ne mélange pas plusieurs sujets dans un même changement.
5. **Avant d'écrire du code non trivial, propose un plan** et attends validation.
6. Si tu n'es pas sûr d'une propriété de configuration ou d'un nom d'artefact, **dis-le** au lieu
   de l'inventer. Une question coûte moins cher qu'une dépendance qui n'existe pas.
7. **Ne dis jamais « c'est fait ».** Dis « la vérification passe » et montre la commande exécutée
   avec sa sortie. Une affirmation n'est pas une preuve.
8. **Avant toute fonctionnalité, écris le plan dans `prompts/NNN-nom.md`** en suivant
   `prompts/000-modele.md`, et attends l'approbation. Exception : les corrections de moins de
   dix lignes.

## Stack (versions épinglées — vérifiées le 29 août 2026)

| Composant | Version | Note |
|---|---|---|
| Java | 25 (LTS) | Boot 4 exige 17 minimum, compatible jusqu'à 26 |
| Spring Boot | 4.1.1 | sortie le 20 août 2026 ; 3.5 est EOL depuis le 30 juin 2026 |
| Spring Framework | 7.0.x | embarqué par le BOM Boot |
| Angular | 22.1.x | sortie initiale juin 2026 |
| TypeScript | 6.0+ | **obligatoire** pour Angular 22 |
| Node | 24 (LTS) | Angular 22 exige Node 22 minimum |
| PostgreSQL | 18 | |
| Keycloak | 26.7.2 | corrige une CVE de prise de contrôle de compte — ne pas descendre en dessous |
| Build back | Maven | |

> Formateur : revérifie ces versions la veille de la session. Si tu en changes une, mets à jour
> **ce tableau ET `compose.yaml`**. Ce fichier est la seule source de vérité du projet ; si les
> deux divergent, l'agent suivra une version fantôme.

## Pièges connus (à lire avant de coder)

### Spring Boot 4 a renommé les starters

La modularisation de Boot 4 a découpé les starters. Les anciens noms existent encore mais sont
dépréciés et disparaîtront. **Utilise les nouveaux :**

| Ancien (Boot 3) | Nouveau (Boot 4) |
|---|---|
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| `spring-boot-starter-web-services` | `spring-boot-starter-webservices` |
| *(aucun, autoconfiguré)* | `spring-boot-starter-flyway` |
| `spring-boot-starter-test` seul | un starter de test **par technologie** |

Chaque techno a désormais son starter de test dédié : `spring-boot-starter-webmvc-test`,
`spring-boot-starter-data-jpa-test`, `spring-boot-starter-graphql-test`,
`spring-boot-starter-webservices-test`. Sans eux, les annotations comme `@AutoConfigureMockMvc`
ou `@DataJpaTest` échouent avec des erreurs de contexte peu lisibles.

Les packages ont suivi : chaque module est sous `org.springframework.boot.<module>`.

### Autres changements Boot 4

- **Jackson 3** est la valeur par défaut. Les imports `com.fasterxml.jackson.*` changent.
- **Spring Security 7** — la configuration a évolué depuis la 6.x.
- **Hibernate 7**, Jakarta EE 11.
- Les endpoints Actuator et propriétés de configuration dépréciés en 3.x ont été **supprimés**.

### Angular 22

- **OnPush est la stratégie de détection par défaut** pour les nouveaux composants. Ne l'ajoute pas
  explicitement, et n'écris pas de code qui suppose « check always ».
- **Zoneless par défaut** — `zone.js` n'est pas installé sur les nouveaux projets.
- **Vitest** est le lanceur de tests, plus Karma.
- **Signal Forms** et les **Resource APIs** (`resource()`, `httpResource()`) sont stables.
- Pas de `NgModule`. Pas de décorateurs `@Input()` / `@Output()` : utilise `input()` et `output()`.
- TypeScript 6 requis.

### Le serveur MCP Angular

Le CLI Angular expose un serveur MCP via `ng mcp`. Branché sur l'agent, il lui donne le contexte
réel du workspace plutôt que ses souvenirs d'entraînement. **À utiliser** pour tout travail front.

## Architecture

```
supportdesk/
├── CLAUDE.md                       # ce fichier
├── compose.yaml                    # dépendances d'infra uniquement
├── .env                            # copié depuis .env.example, jamais commité
├── infra/
│   └── keycloak/
│       └── realm-supportdesk.json  # realm importé au démarrage
├── backend/                        # Spring Boot — tourne depuis l'IDE en dev
└── frontend/                       # Angular — tourne en `ng serve` en dev
```

**En développement, seules les dépendances tournent dans Docker.** Le backend et le frontend
tournent sur la machine, pour garder le hot reload et le debug. Les Dockerfiles applicatifs
sont ajoutés en J4, pour le déploiement.

### Démarrage automatique des dépendances

Le backend inclut le module `spring-boot-docker-compose` (portée `optional`, jamais en
production). Au démarrage de l'application, Spring Boot lance les services de `compose.yaml`
et injecte les `JdbcConnectionDetails` de Postgres — **il n'y a donc aucun
`spring.datasource.url` / `username` / `password` dans `application.yaml`**. Ne les rajoute pas.

Configuration retenue :

```yaml
spring:
  docker:
    compose:
      file: ../compose.yaml
      lifecycle-management: start-only
```

`start-only` est indispensable : la valeur par défaut `start-and-stop` couperait Keycloak à
l'arrêt du backend, alors que le frontend en a besoin. Contrepartie : les conteneurs survivent
à l'arrêt de l'application. Pour les stopper, `docker compose stop` à la racine.

Le module est désactivé pendant les tests (comportement par défaut) — les tests utilisent
Testcontainers.

## Ports

| Service | Port | Accès |
|---|---|---|
| Frontend (`ng serve`) | 4200 | http://localhost:4200 |
| Backend (IDE) | 8080 | http://localhost:8080 |
| Keycloak | 8081 | http://localhost:8081 — admin / admin |
| PostgreSQL | 5432 | base `supportdesk` |
| CRM legacy (SOAP) | 8082 | http://localhost:8082/services |

## Domaine métier

- **Client** — déclare des tickets. Ne voit que **ses** tickets.
- **Agent** — traite les tickets. Voit tous les tickets.
- **Admin** — gère les agents et les référentiels.
- **Ticket** — appartient à un Client, assigné à zéro ou un Agent, porte des Commentaires.
  Statuts : `OUVERT`, `EN_COURS`, `RESOLU`, `FERME`.
- **Commentaire** — auteur, contenu, date, visibilité (public / interne agents).

Les données d'identité des clients (raison sociale, SIRET, contact) ne sont **pas** en base :
elles proviennent du CRM legacy exposé en SOAP. La base ne stocke qu'une référence client
(`crmClientRef`).

## Rôles Keycloak

Realm `supportdesk`. Rôles realm : `CLIENT`, `AGENT`, `ADMIN`.

Utilisateurs de test (mot de passe : `password`) :

| Login | Rôle | Référence CRM |
|---|---|---|
| `alice` | CLIENT | CLI-0001 |
| `david` | CLIENT | CLI-0002 |
| `bob` | AGENT | — |
| `carol` | ADMIN + AGENT | — |

Clients OIDC :
- `supportdesk-front` — public, PKCE S256, redirect `http://localhost:4200/*`
- `supportdesk-api` — audience des tokens, validée par le backend

## Conventions backend

- Packages par domaine : `com.supportdesk.ticket`, `com.supportdesk.client`.
  Pas de découpage technique `controllers/` `services/` `repositories/` à la racine.
- Migrations **Flyway** dans `src/main/resources/db/migration`. `ddl-auto` sur `validate`,
  jamais `update`.
- DTO en `record`. Mapping explicite, pas de mapper magique.
- Validation `jakarta.validation` sur les DTO d'entrée.
- Tests d'intégration avec **Testcontainers**, pas de H2.
- Erreurs centralisées : `@RestControllerAdvice` + `ProblemDetail` (RFC 7807).

## Conventions frontend

- Standalone components, `provideHttpClient()`, routing par `loadComponent`.
- État en **signals**. `httpResource()` en premier réflexe pour les lectures HTTP ;
  pas de `BehaviorSubject` par défaut.
- Modèles TypeScript alignés 1:1 sur les DTO backend, dans `src/app/<domaine>/<domaine>.model.ts`.
- Un interceptor pour le token, un pour les erreurs.
- **Les guards de route sont de l'UX, pas de la sécurité.** Toute règle d'accès doit exister
  côté backend. Ne jamais s'appuyer sur un guard pour protéger une donnée.

## Commandes

```bash
# Infra — utile pour démarrer/arrêter sans lancer le backend
docker compose up -d
docker compose logs -f keycloak
docker compose stop            # `down -v` pour repartir de zéro

# Backend — démarre aussi les dépendances tout seul
cd backend && ./mvnw spring-boot:run
cd backend && ./mvnw test

# Frontend
cd frontend && npm start       # ng serve, proxy vers :8080
cd frontend && npm test        # Vitest
cd frontend && npx ng mcp      # serveur MCP pour l'agent
```
