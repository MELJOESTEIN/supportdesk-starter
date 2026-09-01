# De `git clone` à l'application qui tourne

La suite exacte de commandes, sur une machine vierge. Testée sur Ubuntu 24.04 le
30 août 2026.

Deux chemins, selon ce que tu veux faire :

- **[A. Tout en conteneurs](#a--tout-en-conteneurs)** — une commande, pour voir
  l'application marcher. C'est le mode du J4.
- **[B. Mode développement](#b--mode-développement)** — l'infra en conteneurs, le code sur
  la machine, avec rechargement à chaud. C'est le mode des J1 à J3.

---

## Prérequis

| Outil | Version | Vérifier | Pourquoi cette version-là |
|---|---|---|---|
| Docker + Compose | récent | `docker compose version` | |
| JDK | **25** | `java -version` | Spring Boot 4.1 (mode B seulement) |
| Node | **≥ 24.15** | `node -v` | mode B seulement — voir ci-dessous |

**Le piège de Node.** Le CLI Angular 22 exige `^22.22.3 || ^24.15.0 || >=26.0.0` et
**refuse de démarrer** en dessous, avec un code de sortie 3. « Node 24 LTS » ne suffit pas
comme spécification : un 24.14 échoue. Avec `nvm` :

```bash
nvm install 24.20.0 && nvm use 24.20.0
```

**Le port 5432.** Si un PostgreSQL tourne déjà sur la machine — service système ou
conteneur d'un autre projet — le démarrage échouera sur un conflit de port. Vérifie :

```bash
docker ps --filter publish=5432
```

---

## A · Tout en conteneurs

```bash
git clone <url-du-depot> supportdesk
cd supportdesk
cp .env.example .env

docker compose --profile full up -d --build
```

Le premier build prend **cinq à dix minutes** : trois images à construire, dont deux
compilations complètes. Les suivants sont quasi instantanés (les couches sont en cache).

Attends que tout soit en bonne santé :

```bash
docker compose --profile full ps
# les cinq services doivent afficher (healthy)
```

Puis ouvre **http://localhost:4200** et connecte-toi avec `alice` / `password`.

| Service | Adresse |
|---|---|
| Application | http://localhost:4200 |
| API | http://localhost:8080/api |
| Keycloak | http://localhost:8081 — `admin` / `admin` |
| CRM legacy (WSDL) | http://localhost:8082/services/clients.wsdl |

Pour tout arrêter :

```bash
docker compose --profile full down        # garde les données
docker compose --profile full down -v     # repart de zéro
```

---

## B · Mode développement

```bash
git clone <url-du-depot> supportdesk
cd supportdesk
cp .env.example .env
```

### 1 · Les dépendances

```bash
docker compose up -d
docker compose ps        # postgres, keycloak, legacy-crm : (healthy)
```

Keycloak met **une à deux minutes** au premier démarrage : il construit sa configuration
Quarkus, crée son schéma et importe le realm. `docker compose logs -f keycloak` pour
suivre.

### 2 · Le backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Le module `spring-boot-docker-compose` démarre les conteneurs tout seul si tu as sauté
l'étape 1, et il ne les arrête pas en s'arrêtant (`start-only`) — sinon Keycloak tomberait
alors que le front en dépend. Pour les arrêter : `docker compose stop`.

Le profil `dev` affiche les requêtes SQL. C'est là qu'on voit les N+1.

### 3 · Le frontend

```bash
cd frontend
npm install
npm start
```

http://localhost:4200. Le proxy de développement relaie `/api` et `/graphql` vers `:8080`.

### 4 · Vérifier que la chaîne complète tourne

```bash
# Un jeton, comme le fait le navigateur
curl -s -X POST http://localhost:8081/realms/supportdesk/protocol/openid-connect/token \
  -d grant_type=password -d client_id=supportdesk-front \
  -d username=alice -d password=password | jq -r .access_token
```

Puis ouvre `verif/40-bola.http` dans VS Code (extension REST Client) et joue les requêtes
dans l'ordre. C'est la démonstration du J2.

---

## Ce qui tourne, et ce qu'il faut savoir

### Comptes de test

Mot de passe : `password` pour tous.

| Login | Rôle | Compte CRM | Ce qu'il voit |
|---|---|---|---|
| `alice` | CLIENT | CLI-0001 | ses 8 tickets |
| `david` | CLIENT | CLI-0002 | ses 5 tickets |
| `bob` | AGENT | — | les 25 tickets, les notes internes, le back-office |
| `carol` | ADMIN + AGENT | — | idem |

### Les tests

```bash
cd backend    && ./mvnw test          # 59 tests, dont Testcontainers (Docker requis)
cd frontend   && npm test             # 19 tests
cd frontend   && npm run verifier:jetons
cd legacy-crm && ./mvnw test          # 8 tests
```

### Deux différences entre les modes A et B

**L'issuer OIDC.** Le backend valide `http://localhost:8081/realms/supportdesk` —
l'adresse que voit le **navigateur**, celle qui est inscrite dans le jeton. Mais lui-même
n'a aucune route vers `localhost:8081` depuis son conteneur : il récupère les clés de
signature par `http://keycloak:8080/...`. **Les deux URL sont différentes, et c'est
normal.** Confondre les deux donne « iss claim is not valid » sur des jetons parfaitement
valides — un piège qui ne se manifeste qu'au déploiement.

**CORS.** En mode B, le front est sur `:4200` et l'API sur `:8080` : deux origines, donc
CORS. En mode A, nginx sert les deux : **même origine, plus de CORS du tout**. Une
configuration CORS approximative passe donc inaperçue en production et casse en
développement — l'inverse de ce qu'on croit.

---

## Si ça ne marche pas

| Symptôme | Cause probable |
|---|---|
| `Node.js version vX detected. The Angular CLI requires…` | Node < 24.15 — `nvm install 24.20.0` |
| conteneur `sd-postgres` en `Exited (1)` | un autre PostgreSQL occupe le port 5432 |
| Keycloak reste en `health: starting` | c'est normal une à deux minutes au premier lancement |
| 401 `Jwt expired` sur les `.http` | les jetons durent 5 minutes — rejoue `verif/00-tokens.http` |
| 503 sur `/api/clients/…` | le CRM est arrêté — `docker compose start legacy-crm` |
| `docker compose --profile full up` : port occupé | un `ng serve` ou un backend d'IDE tourne encore |
| `Migration checksum mismatch for migration version 2` | `V2__seed.sql` a changé depuis ton dernier démarrage — voir ci-dessous |
| la page Keycloak est en anglais / titrée « your account » | le realm date d'avant la correction — voir ci-dessous |

### Reprendre depuis une base propre

Les deux dernières lignes du tableau ont la même réponse, et c'est la seule qui marche :

```bash
docker compose down -v && docker compose up -d
```

**Pourquoi.** Flyway refuse de démarrer si le contenu d'une migration déjà appliquée a
changé — c'est une protection, pas un caprice : deux bases portant le même numéro de version
avec des données différentes est exactement ce qu'on ne veut jamais. Et Keycloak
n'importe un realm qu'à sa **création** : `--import-realm` ne réimporte rien sur un realm
existant, donc une modification de `infra/keycloak/realm-supportdesk.json` reste invisible
tant que le volume n'est pas supprimé.

`down -v` supprime les données locales : les tickets de démonstration sont recréés par le
seed, mais **les tickets que tu as créés à la main sont perdus**. C'est sans conséquence sur
un poste de formation, et c'est la remise à zéro documentée pour repartir d'un état connu.

> En production, on ne modifie jamais une migration appliquée : on en ajoute une nouvelle.
> Ici, `V2__seed.sql` n'est qu'un jeu de démonstration et la base se recrée en dix secondes —
> c'est le seul contexte où réécrire une migration se défend. **Dis-le en séance :** un
> étudiant qui retient « on peut modifier une migration » a appris l'inverse de ce qu'il faut.
