# legacy-crm — référentiel clients ACME (SOAP)

## Pour l'étudiant : boîte noire

**Ce module ne s'ouvre pas en séance.** Il simule un système d'information existant, écrit par
quelqu'un d'autre, il y a longtemps, dont on n'a que le contrat. C'est la situation normale en
entreprise : on ne réécrit pas le legacy, on s'y branche.

Tout ce dont tu as besoin est publié par le service lui-même :

```bash
curl -s http://localhost:8082/services/clients.wsdl    # le contrat
# /services seul renvoie 405 : c'est l'adresse d'envoi SOAP, elle n'accepte que POST
```

Si tu ouvres les sources pour comprendre pourquoi un appel échoue, tu t'entraînes à quelque chose
que tu ne pourras pas faire chez toi. Lis le WSDL.

## Pour le formateur

Module Spring Boot 4.1.1 / Java 25, contract-first, construit par `compose.yaml`
(`build: ./legacy-crm`) — il n'y a plus d'image à fabriquer à la main avant la session.

| Élément | Valeur |
|---|---|
| Namespace | `http://legacy.acme.fr/crm` |
| Contrat | `src/main/resources/schemas/clients.xsd` — les classes Java en sont générées par XJC |
| Endpoint SOAP | `POST /services` |
| WSDL | `GET /services/clients.wsdl` (nom du bean `clients`) |
| Santé | `GET /actuator/health` |
| Port | 8080 dans le conteneur, publié sur **8082** |

### Comportements « legacy » volontaires

| Cas | Réponse | Ce que ça enseigne |
|---|---|---|
| `GetClient` sur une référence inconnue | SOAP Fault `CLIENT_INCONNU` | un fault n'est pas un 500 : il se traduit en `ProblemDetail` |
| `SearchClients` avec un motif vide | SOAP Fault `CRITERE_OBLIGATOIRE` | un référentiel ne se laisse pas énumérer |
| Tout appel | **400 ms** de latence | timeouts, accès par lot, cache — le N+1 par le réseau |

La latence est réglable (`legacy-crm.latence`), mais un test la verrouille : la retirer ferait
disparaître le problème que le J3 apprend à résoudre.

### Jeu de données

Huit fiches, dont `CLI-0001` **Transports Nord** (alice) et `CLI-0002` **Ateliers Sud** (david) —
les références portées par les jetons Keycloak — les trois raisons sociales de la maquette
(`Atelier Vernet`, `Groupe Lauziere`, `Merieux et Fils`) et un compte **inactif** (`CLI-0008`).

### Construire et tester hors Compose

```bash
cd legacy-crm && ./mvnw test
docker build -t supportdesk/legacy-crm:1.0 .
```
