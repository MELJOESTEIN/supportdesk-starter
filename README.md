# SupportDesk — dépôt de formation full stack

Projet fil rouge d'une formation de 4 jours : Spring Boot 4.1 · Angular 22 · Keycloak · SOAP ·
GraphQL · Docker · CI/CD, développée avec un agent de codage selon une méthode explicite.

Versions vérifiées le **29 août 2026**. Revérifie-les la veille de la session.

---

## Importer dans VS Code

### 1. Prérequis machine (à valider AVANT la session)

| Outil | Version | Vérifier avec |
|---|---|---|
| JDK | 25 | `java -version` |
| Node | 24 LTS, **≥ 24.15** | `node -v` — le CLI Angular 22 refuse de démarrer en dessous |
| Docker + Compose | récent | `docker compose version` |
| Claude Code | à jour | `claude --version` |

Un Node trop ancien et le CLI Angular refuse de démarrer. Le message est explicite mais le
seuil est précis : `@angular/cli@22.1.6` exige `^22.22.3 || ^24.15.0 || >=26.0.0`. Un Node
24.14 — pourtant « 24 LTS » — ne passe pas. C'est le premier plantage à éliminer.

### 2. Ouvrir le projet

```bash
unzip supportdesk.zip
cd supportdesk
cp .env.example .env
git init && git add -A && git commit -m "chore: socle de formation"
code supportdesk.code-workspace
```

> **Le point critique.** Ouvre **le workspace ou le dossier racine** — jamais `backend/` ou
> `frontend/` seuls. Claude Code lit `CLAUDE.md` et `.claude/` depuis le répertoire où il est
> lancé. Ouvrir `backend/` directement et lancer l'agent dedans, c'est travailler sans contexte :
> il écrira du Spring Boot 3 avec assurance et personne ne comprendra pourquoi.

### 3. Installer les extensions

VS Code proposera les extensions recommandées à l'ouverture. Sinon : palette de commandes →
`Extensions: Show Recommended Extensions` → tout installer.

| Extension | Pourquoi |
|---|---|
| Claude Code | l'agent, intégré à l'éditeur |
| Extension Pack for Java | langage, debug, tests |
| Spring Boot Extension Pack | complétion des propriétés, Boot Dashboard |
| Angular Language Service | complétion dans les templates |
| ESLint + Prettier | conventions front |
| Vitest | Angular 22 utilise Vitest, plus Karma |
| Docker | inspection des conteneurs |
| YAML | `compose.yaml`, `application.yaml` |
| REST Client | tester les endpoints sans quitter l'éditeur |

### 4. Vérifier que l'agent voit bien le contexte

Dans le terminal intégré, **à la racine** :

```bash
claude
```

puis, dans l'agent :

```
/context
```

Tu dois y voir `CLAUDE.md` chargé. Test décisif — pose la question sans donner d'indice :

> Quel starter Spring Boot dois-je utiliser pour du web sur ce projet ?

S'il répond `spring-boot-starter-webmvc`, le contexte fonctionne. S'il répond
`spring-boot-starter-web`, il ne lit pas le fichier : tu es probablement lancé depuis un
sous-dossier.

C'est aussi la démonstration à faire en séance au J1 — bien plus convaincante qu'un discours.

### 5. TypeScript 6

Après le `ng new` du J1, VS Code proposera d'utiliser la version TypeScript du workspace.
**Accepte.** Sinon il valide le code avec sa version embarquée, plus ancienne, et laisse passer
des erreurs qu'Angular 22 rejettera au build.

---

## Ce que contient le dépôt

```
supportdesk/
├── README.md                     ← tu es ici
├── CLAUDE.md                     ← contexte projet, lu par l'agent à chaque session
├── compose.yaml                  ← dépendances d'infra (Postgres, Keycloak, CRM legacy)
├── .env.example                  ← à copier en .env
├── supportdesk.code-workspace    ← à ouvrir dans VS Code
├── skills-lock.json              ← provenance et empreinte des compétences installées
│
├── .agents/skills/               ← compétences partagées, installées depuis angular/skills
│   ├── angular-developer/        ← cible des liens de .claude/skills/
│   └── angular-new-app/
│
├── .claude/
│   ├── skills/
│   │   ├── spring-boot-4/        ← FOURNIE — écarts Boot 3 → Boot 4
│   │   ├── angular-22/           ← SQUELETTE — l'étudiant la complète au J1
│   │   ├── revue-owasp/          ← FOURNIE — audit de sécurité en 5 risques
│   │   ├── spring-boot-rest-api/          ← FOURNIE — REST Boot 4.1 (J1, J2)
│   │   ├── spring-boot-graphql-api/       ← FOURNIE — GraphQL, DataLoader (J3)
│   │   ├── spring-boot-soap-webservices/  ← FOURNIE — SOAP, WSDL, JAXB (J3)
│   │   ├── angular-developer/    ← lien → ../../../.agents/skills/ (angular/skills)
│   │   └── angular-new-app/      ← lien → ../../../.agents/skills/ (angular/skills)
│   ├── rules/securite.md         ← règles chargées seulement sur les fichiers sensibles
│   └── agents/code-reviewer.md   ← sous-agent de revue à froid
│
├── docs/
│   ├── methode-agentique.md      ← document de référence du module M0
│   ├── produit.md                ← SQUELETTE — l'étudiant le remplit au J1
│   ├── audit-securite.md         ← SQUELETTE — son rapport d'audit, J2
│   ├── decision-architecture.md  ← SQUELETTE — son arbitrage REST/GraphQL/SOAP, J3
│   └── contexte-agent.md         ← SQUELETTE — ce qu'il ajouterait au contexte, J4
│
├── prompts/000-modele.md         ← modèle de plan avant code
│
├── design/                       ← maquette Claude Design, à reproduire en Angular
│   ├── README.md                 ← consignes du bundle de handoff
│   └── project/
│       ├── SupportDesk.dc.html   ← les écrans + le système de design
│       ├── tokens.css            ← jetons : couleurs, typo, statuts, note interne
│       └── support.js
│
├── infra/keycloak/               ← realm importé au démarrage
│
└── legacy-crm/                   ← CRM SOAP, construit par Compose — boîte noire, lis le WSDL

     backend/   ← à créer au J1 : Spring Boot 4.1
     frontend/  ← à créer au J1 : Angular 22
```

> **`backend/` et `frontend/` n'existent pas encore, et c'est voulu.** Tu les construis en
> séance. Ce dépôt te donne le contexte, l'infrastructure, les compétences de l'agent et la
> maquette — pas le code.

> **`angular-developer/` et `angular-new-app/` sont des liens symboliques** vers
> `.agents/skills/`. Leur cible est **à l'intérieur** de `supportdesk/` : l'archive de
> l'étudiant reste correcte quelle que soit la façon dont tu la fabriques, et le dossier
> peut être déplacé d'un bloc. Ce sont les deux compétences officielles Angular, verrouillées
> par `skills-lock.json` — les six autres sont écrites pour ce projet.

---

## Ordre de lecture

Dans cet ordre, avant le premier jour :

1. **`docs/methode-agentique.md`** — comment on travaille avec un agent de codage sur ce projet.
   C'est le document de référence de la semaine, à garder ouvert.
2. **`CLAUDE.md`** — le contexte du projet : versions épinglées, conventions, et surtout la
   section « Pièges connus ». L'agent le lit à chaque session ; toi aussi.
3. **[La carte du code](https://MELJOESTEIN.github.io/supportdesk-carte-du-code/)** — où vit
   quoi, et dans quel ordre le code s'écrit.

Puis, au J1, avant d'écrire la moindre ligne : **`docs/produit.md`**. Tu le remplis, l'agent le
critique. Jamais l'inverse.

---

## Démarrer

```bash
cp .env.example .env
docker compose up -d          # Postgres, Keycloak, CRM legacy
docker compose ps             # les 3 services doivent être « healthy »
```

Le premier démarrage construit l'image du CRM legacy : compte quelques minutes.

```bash
docker compose logs -f keycloak   # suivre l'import du realm
docker compose stop               # arrêter ; `down -v` pour repartir de zéro
```

`backend/` et `frontend/` se lancent au J1, une fois créés :

```bash
cd backend  && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
cd frontend && npm start
```

Le backend embarque `spring-boot-docker-compose` en `start-only` : il démarre ces conteneurs
tout seul et ne les arrête pas en s'arrêtant — sinon Keycloak tomberait alors que le front en
dépend.

| Service | URL |
|---|---|
| Frontend | http://localhost:4200 |
| Backend | http://localhost:8080 |
| Keycloak | http://localhost:8081 — `admin` / `admin` |
| CRM legacy (WSDL) | http://localhost:8082/services/clients.wsdl |

Utilisateurs de test, mot de passe `password` : `alice` (CLIENT, CLI-0001) · `david` (CLIENT,
CLI-0002) · `bob` (AGENT) · `carol` (ADMIN).

## Prérequis formateur

Aucun. Le CRM legacy est le module `legacy-crm/`, construit par Compose au premier
`docker compose up -d` (comptez une à deux minutes). Il reste une **boîte noire pédagogique** :
l'étudiant ne l'ouvre pas, il lit son WSDL — voir `legacy-crm/README.md`.
