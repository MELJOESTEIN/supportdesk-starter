# Guide pratique — attaquer et sécuriser son API

**Jour 4, après-midi.** Six manches. Tu attaques ton propre projet, tu constates ce qui tient,
tu trouves ce qui manque, et tu combles un trou.

Toutes les commandes de ce guide ont été exécutées sur ce projet le 3 septembre 2026. Les
sorties montrées sont réelles. Si tu obtiens autre chose, ce n'est pas toi qui as mal copié :
c'est qu'il y a quelque chose à comprendre. Arrête-toi et cherche.

> **Le cadre, et il n'est pas négociable.** Tu attaques *ta* machine, *ton* conteneur, *ton*
> code. Tout ce qui est écrit ici est légal parce que le système t'appartient. Les mêmes
> commandes contre un serveur qui n'est pas le tien sont un délit. Ce n'est pas une formule de
> style : la différence entre un audit et une intrusion est une autorisation écrite.

---

## §0 · Démarrer

```bash
cd supportdesk
docker compose up -d
cd backend && ./mvnw spring-boot:run
```

Attends `Started SupportdeskApplication`. Puis, dans un **autre** terminal :

```bash
cd supportdesk
curl -s -o /dev/null -w 'backend  %{http_code}\n' localhost:8080/actuator/health
curl -s -o /dev/null -w 'keycloak %{http_code}\n' localhost:8081/realms/supportdesk
./verif/crm.sh wsdl | tail -1
```

Attendu :

```
backend  200
keycloak 200
__ HTTP 200 · 4083 octets
```

Les quatre comptes de test, mot de passe `password` :

| compte | rôle | référence CRM |
|---|---|---|
| `alice` | CLIENT | CLI-0001 |
| `david` | CLIENT | CLI-0002 |
| `bob` | AGENT | — |
| `carol` | ADMIN + AGENT | — |

---

## §1 · Le jeton — ce qu'il dit, et ce qu'il ne prouve pas

### 1.1 Ouvre-le

```bash
./verif/jeton.sh alice --claims
```

```
  preferred_username   : alice
  crm_client_ref       : CLI-0001
  realm_access         : ['CLIENT']
  aud                  : supportdesk-api
  iss                  : http://localhost:8081/realms/supportdesk
  exp                  : 1756…
```

**Ce que tu dois retenir :** ce n'est pas déchiffré, c'est **décodé**. Du Base64, rien de plus.
N'importe qui ayant le jeton lit son contenu. Ne mets jamais un secret dans un jeton.

### 1.2 Essaie de le falsifier

Change `CLI-0001` en `CLI-0002` et réencode. La charge utile est valide, le JSON est correct,
et pourtant :

```bash
T=$(./verif/jeton.sh alice)
FAUX=$(python3 - "$T" <<'PY'
import sys, json, base64
h, c, s = sys.argv[1].split('.')
pad = lambda x: x + '=' * (-len(x) % 4)
claims = json.loads(base64.urlsafe_b64decode(pad(c)))
claims['crm_client_ref'] = 'CLI-0002'          # on se donne le compte de david
nouveau = base64.urlsafe_b64encode(json.dumps(claims).encode()).decode().rstrip('=')
print(f"{h}.{nouveau}.{s}")                     # même signature, autre contenu
PY
)
curl -s -o /dev/null -w 'jeton falsifié → %{http_code}\n' localhost:8080/api/tickets -H "Authorization: Bearer $FAUX"
```

```
jeton falsifié → 401
```

**La signature couvre l'en-tête et la charge utile.** Changer un octet de la charge la rend
invalide, et fabriquer la bonne signature demanderait la clé privée de Keycloak — qui ne sort
jamais de son conteneur.

### 1.3 L'audience — la faille invisible

Le même utilisateur, le même Keycloak, mais un jeton demandé **pour une autre application** :

```bash
JI=$(curl -s -X POST localhost:8081/realms/supportdesk/protocol/openid-connect/token \
       -d grant_type=password -d client_id=intranet-front \
       -d username=alice -d password=password \
     | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")

echo "$JI" | cut -d. -f2 | base64 -d 2>/dev/null | python3 -m json.tool | grep '"aud"'
curl -s -o /dev/null -w 'appel SupportDesk → %{http_code}\n' localhost:8080/api/tickets -H "Authorization: Bearer $JI"
```

```
    "aud": "intranet-api",
appel SupportDesk → 401
```

Ce jeton est **authentique** : bonne signature, bon émetteur, non expiré, émis il y a trois
secondes. Il est refusé parce qu'il n'était pas adressé à cette API.

> **À faire, et c'est le geste le plus instructif du guide.** Ouvre
> `backend/src/main/java/com/supportdesk/securite/ConfigurationSecurite.java`, trouve la méthode
> `validateurs(...)`, retire `new ValidateurAudience(audience)`. Relance. Refais l'appel
> ci-dessus.
>
> Tu obtiendras `200`. Et **tous tes écrans continueront de fonctionner**. C'est exactement pour
> ça que cette faille est partout : elle ne se manifeste jamais comme une panne.
>
> Remets la ligne. Puis lance `./mvnw test -Dtest=AudienceCroiseeTests` : quatre tests, et ils
> sont la seule chose qui t'aurait averti.

---

## §2 · REST — la faille numéro un

### 2.1 Balaye

C'est le geste de l'attaquant : on ne teste pas un identifiant, on les essaie tous.

```bash
T=$(./verif/jeton.sh alice)
for i in $(seq 1 12); do
  printf "  ticket %-3s → %s\n" "$i" \
    "$(curl -s -o /dev/null -w '%{http_code}' localhost:8080/api/tickets/$i -H "Authorization: Bearer $T")"
done
```

```
  ticket 1   → 200      ticket 7   → 200
  ticket 2   → 200      ticket 8   → 200
  ticket 3   → 200      ticket 9   → 403
  ticket 4   → 200      ticket 10  → 403
  ticket 5   → 200      ticket 11  → 403
  ticket 6   → 200      ticket 12  → 403
```

Huit tickets, puis un mur. Le mur, c'est **une ligne de Java** dans `TicketService` :

```java
if (!ticket.appartientA(perimetre)) { throw new AccesRefuseException(id); }
```

Aucune configuration Spring ne peut écrire cette ligne à ta place. Spring ne sait pas à qui
appartient un ticket.

### 2.2 Le paramètre qui ne sert à rien

Essaie de te donner le périmètre d'un autre en le demandant poliment :

```bash
curl -s "localhost:8080/api/tickets?crmClientRef=CLI-0002&taille=3" -H "Authorization: Bearer $T" \
 | python3 -c "import sys,json;d=json.load(sys.stdin);print('refs vues :',{t['crmClientRef'] for t in d['contenu']})"
```

```
refs vues : {'CLI-0001'}
```

Le paramètre est **lu et ignoré**. Le périmètre vient de `UtilisateurCourant`, construit à partir
du seul jeton. Regarde `UtilisateurCourantArgumentResolver` : c'est un `record` immuable, et il
n'existe aucun moyen d'en obtenir un qui vienne de la requête.

**La leçon de conception :** on n'a pas compté sur la discipline du développeur. On a rendu le
mauvais geste **impossible à écrire**.

### 2.3 Ce que dit le refus

```bash
curl -s localhost:8080/api/tickets/12 -H "Authorization: Bearer $T"
```

```json
{"detail":"Ce ticket appartient à un autre compte","instance":"/api/tickets/12",
 "status":403,"title":"Accès non autorisé",
 "type":"https://supportdesk.local/erreurs/ticket-autre-compte"}
```

Le test `refus_neDivulgueRien()` vérifie que le sujet, la référence et le compte du ticket ne
fuient pas. Ils ne fuient pas.

> **Mais discute-en, parce que ce n'est pas parfait.** Ce message confirme que le ticket 12
> **existe** et appartient à quelqu'un d'autre. Un `404` n'aurait rien confirmé du tout.
>
> `403` ou `404` ? Les deux se défendent. `403` est honnête et plus facile à déboguer ; `404`
> ferme l'énumération. Ce qui compte, c'est d'avoir **choisi**, et de savoir pourquoi. Note ta
> réponse, tu la défendras devant le formateur.

### 2.4 La donnée qui ne part pas

Même ticket, deux regards :

```bash
B=$(./verif/jeton.sh bob)
for who in "alice:$T" "bob:$B"; do
  n=${who%%:*}; tok=${who#*:}
  printf "  %-6s → %s commentaires, visibilités %s\n" "$n" \
   "$(curl -s localhost:8080/api/tickets/1 -H "Authorization: Bearer $tok" | python3 -c 'import sys,json;print(len(json.load(sys.stdin)["commentaires"]))')" \
   "$(curl -s localhost:8080/api/tickets/1 -H "Authorization: Bearer $tok" | python3 -c 'import sys,json;print(sorted({c["visibilite"] for c in json.load(sys.stdin)["commentaires"]}))')"
done
```

```
  alice  → 3 commentaires, visibilités ['PUBLIC']
  bob    → 5 commentaires, visibilités ['INTERNE', 'PUBLIC']
```

Le filtrage se fait **côté serveur**. Si le backend renvoyait les cinq et que le front en cachait
deux, alice les lirait dans l'onglet Réseau de son navigateur. La donnée serait partie.

---

## §3 · GraphQL — une seule porte, d'autres pièges

### 3.1 Le rôle, à l'entrée

```bash
gql() { curl -s localhost:8080/graphql -H "Authorization: Bearer $1" \
        -H 'Content-Type: application/json' -d "{\"query\":\"$2\"}"; }

curl -s -o /dev/null -w 'alice (CLIENT) → %{http_code}\n' localhost:8080/graphql \
  -H "Authorization: Bearer $T" -H 'Content-Type: application/json' -d '{"query":"{ tickets { total } }"}'
gql "$B" '{ tickets(taille:2) { total contenu { reference statut } } }'
```

```
alice (CLIENT) → 403
{"data":{"tickets":{"total":25,"contenu":[{"reference":"TCK-4790","statut":"EN_COURS"}, …]}}}
```

GraphQL est réservé au back-office agent. La règle tient en une ligne de `ConfigurationSecurite` —
et **c'est tout ce qu'une règle d'URL peut faire ici**, puisqu'il n'y a qu'une seule URL.

### 3.2 Le refus qui arrive en 200

Demande un ticket qui n'est pas à toi, avec un jeton client, en REST puis en GraphQL. Compare
les codes HTTP. C'est le rappel du J3 : **en GraphQL, la requête a réussi ; c'est un champ qui a
échoué.**

### 3.3 La complexité, pour de vrai

```bash
Q=$(python3 -c "print(' '.join([f'a{i}: tickets(taille:1) {{ contenu {{ id reference sujet statut priorite }} }}' for i in range(60)]))")
curl -s localhost:8080/graphql -H "Authorization: Bearer $B" -H 'Content-Type: application/json' \
  -d "$(python3 -c "import json,sys;print(json.dumps({'query':'{ '+sys.argv[1]+' }'}))" "$Q")"
```

```
maximum query complexity exceeded 420 > 200
```

**Trois choses à comprendre ici, et la troisième est la plus importante.**

1. La réponse est un `200`. Le serveur a refusé, HTTP dit que tout va bien.
2. Cette limite n'existe que parce que quelqu'un a écrit
   `new MaxQueryComplexityInstrumentation(200)`. Rien n'est actif par défaut.
3. **La complexité compte les champs écrits dans la requête, pas les lignes renvoyées.** Essaie
   `{ tickets(taille: 100000) { total contenu { id } } }` : complexité minuscule, accepté. Ce
   qui te protège du volume, c'est `Math.min(taille, 100)` dans le contrôleur — une protection
   entièrement différente, contre une menace entièrement différente.

### 3.4 La profondeur — une limite qui ne sert pas encore

```java
new MaxQueryDepthInstrumentation(10)
```

Essaie d'écrire une requête de profondeur 11. **Tu n'y arriveras pas.** `Commentaire` n'a aucun
champ qui redescende vers `Ticket` : le schéma n'a pas de cycle, et la profondeur maximale
atteignable est 4.

Cette protection est donc inutile aujourd'hui. Garde-la quand même, et comprends pourquoi : le
jour où quelqu'un ajoutera un champ `ticket: Ticket!` sur `Commentaire` — dix secondes de
travail, aucun rapport apparent avec la sécurité — le cycle existera. **Une limite se pose avant
d'en avoir besoin, parce qu'après, personne n'y pense.**

### 3.5 La carte de l'API

```bash
gql "$B" '{ __schema { types { name } } }' | head -c 200
```

Ça marche : l'introspection est active en développement, c'est ce qui rend GraphiQL utilisable
sur <http://localhost:8080/graphiql>.

Maintenant regarde `application-prod.yaml` : `introspection.enabled: false`,
`graphiql.enabled: false`, et Swagger éteint dans la foulée. **En production, une API ne publie
pas sa propre carte.**

### 3.6 Le champ sans annotation — cherche la faille

Ouvre `TicketGraphQlController.java`. Compte les `@PreAuthorize` : il y en a **sept**, sur trois
requêtes et quatre mutations.

Puis regarde les résolveurs de champs — `client`, `assigneA`, `commentaires`. **Aucun n'est
annoté.** Est-ce une faille ?

Réponse : non, et pour deux raisons qu'il faut savoir énoncer. Ils ne sont atteignables qu'à
travers une requête racine déjà protégée, **et** `commentaires` repasse par
`tickets.detail(id, utilisateur)` — le service, qui revérifie le propriétaire et filtre les
notes internes.

> **Le vrai danger est là.** Le jour où quelqu'un « optimise » ce résolveur en appelant
> directement le dépôt pour éviter un aller-retour — un geste banal, que tu feras un jour — les
> notes internes s'ouvrent à tout le monde. Sans qu'une seule annotation ait disparu.
>
> La protection n'est pas dans l'annotation. Elle est dans le fait de **repasser par le
> service**.

---

## §4 · SOAP — ce qu'on contrôle quand on est le client

Ici SupportDesk n'est pas le serveur : il est **client** d'un CRM de 2009 qui ne connaît pas
OAuth2 et qu'on n'a pas le droit de modifier. On ne sécurise donc pas l'authentification. On
sécurise ce qu'on envoie, ce qu'on accepte, et à qui on parle.

### 4.1 XXE — la faille du XML

```bash
./verif/crm.sh brut '<!DOCTYPE r [<!ENTITY x SYSTEM "file:///etc/hostname">]><GetClientRequest xmlns="http://legacy.acme.fr/crm"><clientRef>&x;</clientRef></GetClientRequest>'
```

Un parseur XML non bridé **résout l'entité** et lit le fichier. C'est dans la norme XML depuis
1998, et c'est activé par défaut dans la plupart des bibliothèques historiques.

Regarde ensuite notre côté, dans `ConfigurationCrm.java` :

```java
marshaller.setSupportDtd(false);
marshaller.setProcessExternalEntities(false);
```

**Deux lignes.** Retire-les : aucun test ne devient rouge, aucun écran ne casse. C'est la
signature d'une protection qu'on oublie.

### 4.2 SSRF — l'adresse ne vient jamais de l'appelant

```yaml
supportdesk.crm.url: http://localhost:8082/services
```

Imagine que cette URL soit un paramètre de requête. L'appelant ferait joindre par **ton
serveur** une adresse que lui ne peut pas atteindre : la base de données interne, un service
d'administration, les métadonnées du fournisseur cloud.

**Une adresse cible vient de la configuration. Toujours.** La règle vaut pour SOAP, pour un
appel REST sortant, pour un webhook, pour tout.

### 4.3 La façade

```bash
grep -rn "^import.*\(soap\|jaxb\)" backend/src/main/java --include=*.java | grep -v "/client/"
```

Aucun résultat. Rien en dehors du paquet `client` ne connaît SOAP. Une exception `SOAPFault`
arrive en `HTTP 500` du CRM et ressort en `404` ou `400` propre côté API.

**Pourquoi c'est un sujet de sécurité :** une erreur du legacy qui traverserait la façade telle
quelle exposerait des noms de classes, des chemins, une version. La façade est une frontière de
confidentialité autant qu'une frontière technique.

---

## §5 · Configurer Claude Code pour auditer ton API

Un scanner générique trouve les en-têtes manquants et les versions vulnérables. Il ne trouvera
**jamais** que le ticket 12 n'appartient pas à alice — il faudrait connaître ton métier.

Un agent, lui, peut l'apprendre. Voilà les quatre leviers, du plus faible au plus fort.

### 5.1 Les quatre leviers

| Levier | Se charge | L'agent peut-il passer outre ? |
|---|---|---|
| `CLAUDE.md` | à chaque tour | oui, il peut dériver |
| `.claude/rules/` | sur les fichiers désignés | oui |
| `.claude/skills/` | quand on l'invoque | oui, s'il ne la charge pas |
| **hook** | **automatiquement** | **non** |

**Les trois premiers sont des consignes. Le quatrième est une barrière.** Ne les confonds
jamais.

### 5.2 Ce que ce projet a déjà

```bash
cat CLAUDE.md
cat .claude/rules/securite.md
cat .claude/skills/revue-owasp/SKILL.md
cat .claude/agents/code-reviewer.md
```

Lis-les vraiment, ils sont courts. Note en particulier, dans `.claude/rules/securite.md` :

> *La référence client vient du jeton, jamais de la requête.*

C'est la règle que tu as vérifiée en §2.2. Elle est écrite pour l'humain **et** pour l'agent.

### 5.3 Fais-lui trouver ce que tu as trouvé

Dans Claude Code, sur ce projet :

```
Charge .claude/skills/revue-owasp/SKILL.md, puis audite
backend/src/main/java/com/supportdesk/ticket/TicketService.java.

Pour chaque méthode publique qui renvoie une donnée : dis à qui elle appartient,
et cite la ligne exacte où le propriétaire est vérifié. Si tu ne trouves pas la
ligne, dis-le — ne suppose pas qu'elle existe.
```

**Ce qui compte n'est pas sa conclusion. C'est qu'il cite des numéros de ligne.** Un audit sans
référence de ligne est une rédaction, pas un audit. Vérifie deux de ses citations au hasard :
ouvre le fichier, va à la ligne. Si elle ne correspond pas, tu viens d'apprendre la chose la
plus utile de la journée sur les agents.

### 5.4 Le relecteur qui n'a pas écrit le code

```
Lance le subagent code-reviewer sur mes modifications non commitées.
```

Il démarre dans un contexte **vide** : il n'a pas vu la conversation qui a produit le code, donc
il n'a aucune raison de le défendre. C'est tout l'intérêt. Un agent qui relit sa propre
production a déjà décidé qu'elle était bonne.

### 5.5 Écris ta propre skill

C'est l'exercice du jour. Crée `.claude/skills/audit-endpoint/SKILL.md` :

```markdown
---
name: audit-endpoint
description: Vérifie qu'un endpoint respecte les règles de sécurité du projet. À charger avant d'écrire ou de relire un contrôleur.
---

# Audit d'un endpoint

Réponds à ces six questions dans l'ordre, en citant un numéro de ligne à chaque fois.
Si tu ne peux pas citer de ligne, écris « ABSENT » — jamais « probablement géré ailleurs ».

1. Quel rôle cet endpoint exige-t-il, et où est-ce écrit ?
2. La donnée renvoyée appartient-elle à quelqu'un ? Où le propriétaire est-il vérifié ?
3. L'identité de l'appelant vient-elle du jeton, ou d'un paramètre de requête ?
4. Le type de retour est-il une entité JPA ? (interdit) Un DTO ? Lequel ?
5. Le DTO d'entrée accepte-t-il un champ que l'appelant ne devrait pas fixer ?
6. Que renvoie l'erreur, et divulgue-t-elle l'existence de la ressource ?

Termine par : COUVERT, TROU, ou À VÉRIFIER PAR UN HUMAIN.
Ne conclus jamais COUVERT sans avoir cité six lignes.
```

Puis invoque-la sur `ClientController.java`. Compare ce qu'elle produit à ce que tu obtiens en
demandant simplement « est-ce que c'est sécurisé ? ». La différence est l'intérêt de la journée.

### 5.6 La barrière : un hook

Les trois leviers précédents sont des consignes — un agent peut dériver. Un hook, non : c'est
une commande que Claude Code exécute automatiquement.

Crée `.claude/settings.json` :

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          {
            "type": "command",
            "command": "grep -rn '@RequestParam.*[cC]lientRef' backend/src/main/java --include=*.java && echo 'ALERTE : une référence client arrive par la requête. Elle doit venir du jeton.' || true"
          }
        ]
      }
    ]
  }
}
```

> **Vérifie ce hook avant d'y compter.** La configuration des hooks évolue avec les versions de
> Claude Code : lance `/hooks` dans Claude Code pour voir la syntaxe exacte que ta version
> attend, et teste-le en modifiant volontairement un fichier. **Un hook qu'on n'a pas vu se
> déclencher n'existe pas** — c'est exactement la leçon du test de sécurité au §6.4.

---

## §6 · Combler un trou — la limitation de débit

### 6.1 Constate qu'il n'y en a pas

```bash
time (for i in $(seq 1 200); do
  curl -s -o /dev/null localhost:8080/api/tickets -H "Authorization: Bearer $T"
done)
```

Deux cents appels authentifiés, aucun refus. Maintenant imagine la même boucle sur
`/api/tickets/{id}` avec un identifiant qui varie — c'est exactement le balayage du §2.1, et
rien ne le ralentit.

```bash
grep -rn "RateLimit\|Bucket4j" backend/src/main/java backend/pom.xml
```

Aucun résultat. **C'est un vrai trou, dans un projet par ailleurs soigneux.**

### 6.2 Ce que tu dois décider avant de coder

Ne saute pas cette étape, c'est là qu'est la difficulté :

- **On limite quoi ?** Par utilisateur, ou par adresse IP ? (Derrière un proxy, l'IP est celle
  du proxy. Le `sub` du jeton, lui, est fiable.)
- **Combien, sur quelle fenêtre ?** Un chiffre qui gêne un attaquant sans gêner un agent qui
  travaille.
- **On répond quoi ?** `429 Too Many Requests`, avec un en-tête `Retry-After`.
- **Que se passe-t-il au redémarrage ?** Un compteur en mémoire repart à zéro. Acceptable ici ?

### 6.3 Implémente

Trois voies, par difficulté croissante. Choisis-en une :

1. Un `OncePerRequestFilter` avec une `ConcurrentHashMap` et une fenêtre glissante. Aucune
   dépendance, quarante lignes, et tu comprends chaque ligne.
2. **Bucket4j** — la bibliothèque de référence, algorithme du seau à jetons.
3. Une limitation au niveau du reverse proxy nginx. La bonne réponse en production, et celle
   qui n'apprend rien sur Spring.

**Fais la 1 aujourd'hui.** L'objectif est de comprendre, pas de livrer.

### 6.4 Prouve-le

Un test qui vérifie que ça marche ne vaut rien. Écris celui qui vérifie que **ça refuse** :

```java
@Test
@DisplayName("au-delà du quota, l'API répond 429")
void auDelaDuQuota_429() {
    // … N appels acceptés, le N+1 refusé
}
```

Puis le geste qui compte, et sans lequel tu n'as rien prouvé : **désactive ton filtre et relance
le test.** Il doit devenir rouge. S'il reste vert, il ne testait rien.

> Un test de sécurité qu'on n'a jamais vu échouer est une décoration.

---

## Récapitulatif — ce que tu dois pouvoir dire ce soir

1. Un jeton est **signé, pas chiffré**. Tout le monde le lit ; personne ne peut le refaire.
2. **Un jeton valide n'est pas un jeton pour vous.** L'audience se vérifie à la main.
3. Le rôle ouvre une **fonction**. La donnée se vérifie **au plus près d'elle**, dans le service.
4. **L'identité vient du jeton, jamais de la requête** — et on rend l'inverse impossible à
   écrire.
5. Ce qui n'est pas actif par défaut ne s'active pas tout seul : audience, complexité,
   profondeur, DTD, plafond de pagination.
6. Un agent n'est pas un expert en sécurité. C'est **un relecteur à qui on a appris vos règles**,
   et il faut vérifier ses citations.
7. Un test de sécurité teste que **ça refuse**.

---

## Les explorateurs, pour continuer seul

| | |
|---|---|
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| OpenAPI brut | <http://localhost:8080/v3/api-docs> |
| GraphiQL | <http://localhost:8080/graphiql> |
| WSDL du CRM | <http://localhost:8082/services/clients.wsdl> |
| Keycloak | <http://localhost:8081> — `admin` / `admin` |

Les trois premiers sont **éteints en production** par `application-prod.yaml`. Vérifie-le.
