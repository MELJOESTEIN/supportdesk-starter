# SOAP de bout en bout, en ligne de commande

Du contrat brut jusqu'à l'écran, en passant par les erreurs, la lenteur et la panne. Chaque étape
a une **sortie attendue** : si tu obtiens autre chose, arrête-toi, tu viens de trouver quelque
chose.

Toutes les sorties de ce guide ont été obtenues le **2 septembre 2026**.

---

## 0 · Le décor

Le CRM legacy est un système existant. Il expose deux opérations en SOAP, répond en XML, met
**400 ms** à chaque appel, et **tu n'as pas le droit de le modifier**. C'est la situation la plus
fréquente de ta vie professionnelle.

```bash
docker compose up -d
docker compose ps          # sd-legacy-crm doit être « healthy »
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Deux raccourcis à coller dans ton terminal — tout le guide s'appuie dessus :

```bash
env() { echo "<?xml version=\"1.0\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>$1</soap:Body></soap:Envelope>"; }

soap() { curl -s -X POST http://localhost:8082/services \
           -H 'Content-Type: text/xml;charset=UTF-8' \
           -w "\n__ HTTP %{http_code} · %{time_total}s\n" -d "$(env "$1")"; }
```

> **Ne lis pas les sources de `legacy-crm/`.** Elles sont dans le dépôt pour que l'infrastructure
> démarre sans préparation, mais un vrai legacy ne t'ouvre pas son code. Tu as le WSDL — c'est tout
> ce dont tu as besoin, et t'entraîner à t'en contenter est l'objet de la journée.

---

## 1 · Lire le contrat

Un service SOAP publie sa propre documentation. C'est le point de départ.

```bash
curl -s -o /dev/null -w "WSDL     %{http_code} · %{size_download} octets\n" localhost:8082/services/clients.wsdl
curl -s -o /dev/null -w "URL nue  %{http_code}\n" localhost:8082/services
```

**Attendu : `200 · 4083 octets`, puis `405`.**

> **Le piège d'URL.** `/services` est une **adresse d'envoi**, pas une page : elle n'accepte que
> `POST`. Beaucoup d'étudiants testent l'URL nue, voient `405` et croient le service cassé.

### Les opérations disponibles

```bash
curl -s localhost:8082/services/clients.wsdl | grep -oE '<wsdl:operation name="[^"]+"'
```

**Attendu : `GetClient` et `SearchClients`.** Deux opérations, c'est tout ce que ce service sait
faire.

### La forme des données

```bash
curl -s localhost:8082/services/clients.wsdl | grep -oE '<xs:element name="[A-Za-z]+"' | sort -u
```

**Attendu :** `GetClientRequest`, `GetClientResponse`, `SearchClientsRequest`,
`SearchClientsResponse`, et les champs `clientRef`, `raisonSociale`, `siret`, `contactEmail`,
`contactTel`, `actif`, `namePattern`, `client`.

**Trois questions à te poser avant d'écrire la moindre ligne de code :**

1. Quelles opérations existent, et laquelle refuse d'être appelée sans critère ?
2. Que se passe-t-il si la référence client n'existe pas ?
3. Combien de temps le service met-il à répondre ?

Le WSDL répond à la première. Les deux autres se découvrent en essayant — étapes 3 et 4.

---

## 2 · Appeler les deux opérations

### `GetClient` — une référence, une fiche

```bash
soap '<GetClientRequest xmlns="http://legacy.acme.fr/crm"><clientRef>CLI-0001</clientRef></GetClientRequest>'
```

**Attendu :** du XML contenant

```xml
<ns2:raisonSociale>Transports Nord</ns2:raisonSociale>
<ns2:siret>48291736500017</ns2:siret>
<ns2:contactEmail>contact@transports-nord.fr</ns2:contactEmail>
<ns2:actif>true</ns2:actif>
__ HTTP 200 · 0.43s
```

**Trois choses à remarquer.** On n'écrit pas `GET /clients/CLI-0001` : on **emballe une opération
nommée**. Le namespace `http://legacy.acme.fr/crm` est obligatoire — sans lui, le service ne
reconnaît pas la requête. Et le préfixe `ns2:` de la réponse n'a aucune importance : seul le
namespace compte, pas le préfixe choisi pour l'écrire.

### `SearchClients` — un motif, plusieurs fiches

```bash
soap '<SearchClientsRequest xmlns="http://legacy.acme.fr/crm"><namePattern>atelier</namePattern></SearchClientsRequest>'
```

**Attendu :** deux fiches — `Ateliers Sud` et `Atelier Vernet` — en ~0,5 s.

---

## 3 · Les faults, et la surprise du code HTTP

C'est l'étape la plus instructive du guide.

```bash
echo "--- référence inconnue ---"
soap '<GetClientRequest xmlns="http://legacy.acme.fr/crm"><clientRef>CLI-9999</clientRef></GetClientRequest>'

echo "--- motif de recherche vide ---"
soap '<SearchClientsRequest xmlns="http://legacy.acme.fr/crm"><namePattern></namePattern></SearchClientsRequest>'
```

**Attendu :**

```xml
<faultcode>SOAP-ENV:Client</faultcode>
<faultstring xml:lang="en">CLIENT_INCONNU</faultstring>
__ HTTP 500

<faultcode>SOAP-ENV:Client</faultcode>
<faultstring xml:lang="en">CRITERE_OBLIGATOIRE</faultstring>
__ HTTP 500
```

> **Regarde le code HTTP : `500`.**
>
> Une référence inconnue n'est pas une panne serveur — c'est une réponse **prévue par le
> contrat**. Et pourtant SOAP la transporte dans un `500`. C'est le protocole : le fault vit dans
> le corps XML, et le code HTTP ne veut rien dire de plus que « il y a un fault ».
>
> **C'est exactement ce que ta façade devra corriger.** Laisser ce `500` remonter jusqu'au front
> ferait passer une règle métier pour un incident — et déclencherait une astreinte à 3 h du matin
> pour un client qui a tapé une mauvaise référence.

### Une opération qui n'existe pas

```bash
soap '<InconnueRequest xmlns="http://legacy.acme.fr/crm"><x>1</x></InconnueRequest>'
```

**Attendu : `404`.** Pas de fault : le service ne reconnaît même pas la demande. À distinguer du
cas précédent, où l'opération existait mais la donnée non.

---

## 4 · La lenteur, mesurée

```bash
for i in 1 2 3; do
  soap '<GetClientRequest xmlns="http://legacy.acme.fr/crm"><clientRef>CLI-0002</clientRef></GetClientRequest>' | tail -1
done
```

**Attendu : trois fois ~0,43 s.**

> Si ton **tout premier** appel après le démarrage du conteneur met 2 s, c'est normal : la JVM du
> CRM s'échauffe. Relance, tu retrouveras les 0,43 s.

Cette latence est volontaire : elle imite un vrai système
existant, et elle est **verrouillée par un test** côté CRM. La retirer ferait disparaître le
problème que la journée apprend à résoudre.

Fais le calcul tout de suite, il servira à l'étape 9 :

```
un écran qui affiche 25 tickets avec le nom de leur client
25 appels × 400 ms  =  10 secondes
```

---

## 5 · Le durcissement XML

Le XML a une capacité que JSON n'a pas : **inclure un fichier du serveur**. C'est la faille XXE.

```bash
curl -s -X POST http://localhost:8082/services -H 'Content-Type: text/xml;charset=UTF-8' \
  -w "\n__ HTTP %{http_code}\n" \
  -d '<?xml version="1.0"?>
<!DOCTYPE r [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body>
<GetClientRequest xmlns="http://legacy.acme.fr/crm"><clientRef>&xxe;</clientRef></GetClientRequest>
</soap:Body></soap:Envelope>'
```

**Attendu : `500`, et surtout aucune trace de `root:` dans la réponse.** Le contenu de
`/etc/passwd` n'est pas remonté.

Côté **client** — c'est-à-dire dans ton backend — la protection est explicite. Va la lire :

```bash
grep -n "setSupportDtd\|setProcessExternalEntities" \
  backend/src/main/java/com/supportdesk/client/ConfigurationCrm.java
```

**Attendu :**

```java
marshaller.setSupportDtd(false);
marshaller.setProcessExternalEntities(false);
```

> Ces deux lignes ne servent à rien tant que tout va bien. Elles servent le jour où le service
> d'en face est compromis et te renvoie du XML piégé. **On ne fait pas confiance à un XML qu'on
> n'a pas écrit** — même quand il vient d'un partenaire.

---

## 6 · La façade — ce que ton backend en fait

Le front ne parle pas SOAP et ne doit pas avoir à le faire. Ton backend traduit.

```bash
BOB=$(cd verif && ./jeton.sh bob)

curl -s -o /dev/null -w "nominal            %{http_code}\n" -H "Authorization: Bearer $BOB" localhost:8080/api/clients/CLI-0001
curl -s -o /dev/null -w "référence inconnue %{http_code}\n" -H "Authorization: Bearer $BOB" localhost:8080/api/clients/CLI-9999
curl -s -o /dev/null -w "motif vide         %{http_code}\n" -H "Authorization: Bearer $BOB" "localhost:8080/api/clients?recherche="
curl -s -o /dev/null -w "recherche          %{http_code}\n" -H "Authorization: Bearer $BOB" "localhost:8080/api/clients?recherche=atelier"
```

**Attendu : `200`, `404`, `400`, `200`.**

**Compare avec l'étape 3.** Le CRM répondait `500` dans les deux cas d'erreur. Ta façade a traduit :

| Le CRM répond | Ta façade répond | Pourquoi |
|---|---|---|
| fault `CLIENT_INCONNU` · HTTP 500 | **404** | la ressource n'existe pas |
| fault `CRITERE_OBLIGATOIRE` · HTTP 500 | **400** | la demande était mal formée |
| ne répond pas du tout | **503** | ce n'est pas la faute de l'appelant |

Et le corps est un `ProblemDetail`, pas du XML :

```bash
curl -s -H "Authorization: Bearer $BOB" localhost:8080/api/clients/CLI-9999
```

**Attendu :**

```json
{"detail":"Aucune fiche client pour la référence CLI-9999",
 "instance":"/api/clients/CLI-9999","status":404,
 "title":"Fiche client introuvable",
 "type":"https://supportdesk.local/erreurs/client-crm-inconnu",
 "clientRef":"CLI-9999"}
```

### La preuve que la façade est étanche

```bash
# quels fichiers, hors du package client/, prononcent seulement le mot ?
grep -rli "soap\|jaxb" backend/src/main/java --include="*.java" | grep -v "/client/"

# et la vraie question : y a-t-il un import, donc un couplage ?
grep -rni "^import.*\(soap\|jaxb\)" backend/src/main/java --include="*.java" | grep -v "/client/"
```

**Attendu :** la première commande renvoie **deux fichiers** — `GestionnaireErreurs.java` et
`TicketGraphQlController.java`. La seconde ne renvoie **rien**.

Va lire les deux occurrences : ce sont des **commentaires**. Aucun import, aucun type SOAP ne sort
du package `client/`. Le reste de l'application ignore que ce protocole existe.

> C'est le motif le plus réutilisable de la semaine. `CrmClientSoap` absorbe le XML, les faults et
> la lenteur ; `RepertoireClients` expose une interface Java propre ; le front n'en sait rien.
> **Tu appliqueras ça chez toi dès lundi, sur un tout autre système.**

---

## 7 · La façade porte aussi les règles d'accès

Une fiche client n'est pas publique.

```bash
ALICE=$(cd verif && ./jeton.sh alice)
curl -s -o /dev/null -w "alice sur SA fiche      %{http_code}\n" -H "Authorization: Bearer $ALICE" localhost:8080/api/clients/CLI-0001
curl -s -o /dev/null -w "alice sur celle de david %{http_code}\n" -H "Authorization: Bearer $ALICE" localhost:8080/api/clients/CLI-0002
```

**Attendu : `200` puis `403`.**

Le CRM, lui, aurait répondu aux deux sans poser de question — il ne sait pas qui appelle. **La
règle d'appartenance n'existe que dans ta façade.** C'est une raison de plus de ne jamais exposer
le legacy directement.

---

## 8 · Le cache, et ce qu'il masque

```bash
for i in 1 2 3; do
  printf "appel %s : " $i
  curl -s -o /dev/null -w "%{time_total}s\n" -H "Authorization: Bearer $BOB" localhost:8080/api/clients/CLI-0003
done
```

**Attendu :**

```
appel 1 : 0.46s     ← traverse le CRM
appel 2 : 0.02s     ← sort du cache
appel 3 : 0.02s
```

**Vingt-trois fois plus rapide.** Le cache est déclaré par une seule annotation :

```bash
grep -n "@Cacheable" backend/src/main/java/com/supportdesk/client/CrmClientSoap.java
```

---

## 9 · Quand le legacy meurt

Le geste le plus important du guide.

> **Redémarre d'abord le backend.** Le cache vit en mémoire ; sans ça, tout répondra `200` et tu
> ne verras rien. Ctrl+C dans le terminal du backend, puis relance-le.

```bash
docker compose stop legacy-crm
```

Puis :

```bash
curl -s -o /dev/null -w "la liste des tickets     %{http_code}\n" -H "Authorization: Bearer $BOB" localhost:8080/api/tickets
curl -s -o /dev/null -w "une fiche jamais lue     %{http_code}\n" -H "Authorization: Bearer $BOB" localhost:8080/api/clients/CLI-0007
```

**Attendu : `200` puis `503`.**

La liste **continue de s'afficher** — avec les références brutes `CLI-0001` au lieu des raisons
sociales. Seule la fiche, qui ne peut pas exister sans le CRM, échoue.

```bash
curl -s -H "Authorization: Bearer $BOB" localhost:8080/api/clients/CLI-0007
```

**Attendu :**

```json
{"detail":"Le référentiel clients est momentanément indisponible",
 "status":503,"title":"Référentiel clients indisponible",
 "type":"https://supportdesk.local/erreurs/crm-indisponible"}
```

> **Un référentiel injoignable ne doit pas emporter ton back-office.** Un confort d'affichage ne
> devient jamais une dépendance dure. C'est une décision d'architecture, et elle se prend avant la
> panne, pas pendant.

### La question qui n'a pas de réponse évidente

Relance maintenant une fiche **déjà consultée** avant la coupure :

```bash
curl -s -o /dev/null -w "une fiche en cache       %{http_code}\n" -H "Authorization: Bearer $BOB" localhost:8080/api/clients/CLI-0003
```

**Attendu : `200`** — alors que le CRM est mort depuis une minute.

Trois questions, sans bonne réponse unique :

- Est-ce une bonne nouvelle ?
- Combien de temps ça peut durer ?
- L'utilisateur sait-il qu'il regarde une donnée peut-être périmée ?

```bash
docker compose start legacy-crm     # ne l'oublie pas
```

---

## 10 · Le code généré — on n'écrit pas les objets à la main

Le contrat est la source de vérité. Les classes Java en sont **dérivées**, à chaque build.

```bash
cat backend/pom.xml | grep -A10 "jaxb2-maven-plugin"
```

**Attendu :**

```xml
<sources><source>src/main/resources/contrats/clients.xsd</source></sources>
<packageName>com.supportdesk.client.contrat</packageName>
```

Les classes produites :

```bash
find backend/target/generated-sources -name "*.java" | xargs -n1 basename | sort
```

**Attendu : sept fichiers** — `Client`, `GetClientRequest`, `GetClientResponse`,
`SearchClientsRequest`, `SearchClientsResponse`, `ObjectFactory`, `package-info`.

Ils ne sont **pas** dans le dépôt : ils sont régénérés. Change le XSD, relance le build, les
classes suivent.

> **Sur Java 25, `xjc` n'est plus dans le JDK** (JEP 320) : c'est devenu une dépendance de build.
> Un projet qui compilait en Java 11 ne compile plus sans ce plugin.

---

## 11 · Les six pièces, et où les lire

| Pièce | Fichier | Ce qu'elle fait |
|---|---|---|
| Le starter | `pom.xml` | `spring-boot-starter-webservices` — **sans tiret** en Boot 4 |
| Le client HTTP | `pom.xml` | `httpclient5` — le starter n'en embarque **aucun**, et sans lui pas de timeout |
| La génération | `pom.xml` | `jaxb2-maven-plugin`, XSD → 7 classes |
| Le marshaller | `client/ConfigurationCrm.java` | objet ↔ XML, et le durcissement XXE |
| Le template | `client/ConfigurationCrm.java` | `WebServiceTemplate` + les timeouts |
| **La frontière** | `client/CrmClientSoap.java` | **le seul fichier qui sache que le CRM parle XML** |

Les timeouts, à lire dans `application.yaml` :

```yaml
supportdesk:
  crm:
    url: http://localhost:8082/services
    connect-timeout: 2s
    read-timeout: 5s     # le CRM répond en 400 ms ; 5 s laisse de la marge
```

> **Un legacy lent sans timeout, c'est ton API qui tombe avec lui.** Les threads restent bloqués à
> attendre, la file se remplit, et un service tiers en panne devient ta panne.

---

## Ce que tu dois savoir dire à la fin

- Pourquoi `curl localhost:8082/services` renvoie `405`, et où se lit le contrat.
- Pourquoi un fault `CLIENT_INCONNU` arrive en HTTP `500`, et pourquoi ta façade le traduit en `404`.
- Quel fichier de ton backend connaît le XML, et comment tu le prouves.
- Ce qui se passe quand le CRM tombe, et pourquoi la liste des tickets tient quand même.
- Où sont les timeouts, et ce qui arriverait sans eux.

Si l'une de ces questions te laisse sans réponse, reprends l'étape correspondante — le guide est
fait pour être rejoué.
