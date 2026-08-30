---
name: spring-boot-rest-api
description: Construire, sécuriser, tester et exploiter une API REST moderne avec Spring Boot 4.1 / Spring Framework 7 (Java 21-25). Couvre le versionnage d'API natif, Jackson 3, ProblemDetail RFC 9457, les HTTP Service Clients, la résilience, Spring Security 7, l'observabilité, les slices de test et les contraintes propres aux clients mobiles. À utiliser dès qu'il s'agit de REST API, @RestController, controller Spring, DTO/record, endpoint HTTP, API versioning, RestClient, WebClient, ProblemDetail, @WebMvcTest, MockMvcTester, OAuth2 resource server, actuator, migration Spring Boot 3.5 vers 4.x, ou d'un backend REST pour application web ou mobile.
---

# Spring Boot 4.1 — API REST modernes

> État de l'écosystème vérifié en **août 2026**. GA courante : **Spring Boot 4.1.1** / **Spring Framework 7.0.9**.
> Toute la ligne **3.x est hors support OSS depuis le 30/06/2026**. Cible par défaut : **4.1** (support OSS jusqu'à ~07/2027).

## Quand utiliser cette skill

Dès qu'on conçoit, code, revoit, sécurise, teste ou migre une API REST Spring Boot — y compris le backend REST d'une app mobile ou d'un SPA.

---

## 1. Socle et versions (à ne jamais inventer)

| Élément | Valeur |
|---|---|
| Java | baseline **17**, supporté 17→26, **recommandé 25** (cache AOT / Leyden) |
| Spring Framework | 7.0.9 |
| Spring Security | 7.1.0 |
| Spring Data BOM | 2026.0.0 |
| Micrometer / Tracing | 1.17.0 / 1.7.0 |
| Servlet | **6.1** (Tomcat 11.0.x, Jetty 12.1.x) |
| Jakarta Validation | 3.1 · Persistence 3.2 |
| Maven / Gradle | 3.6.3+ / 8.14+ ou 9.x |
| GraalVM | CE 25+, Native Build Tools 1.1.8 |
| springdoc-openapi | **3.1.0** (ligne Boot 4 ; 2.9.0 = Boot 3) |

**Undertow a été supprimé** (incompatible Servlet 6.1) → Tomcat ou Jetty uniquement.

### Renommage des starters (Boot 4)

| Boot 3.5 | Boot 4 |
|---|---|
| `spring-boot-starter-web` | **`spring-boot-starter-webmvc`** (ancien déprécié) |
| `spring-boot-starter-aop` | `spring-boot-starter-aspectj` |
| `spring-boot-starter-oauth2-resource-server` | `spring-boot-starter-security-oauth2-resource-server` |
| — | `spring-boot-starter-restclient`, `spring-boot-starter-webclient`, `spring-boot-starter-jackson`, `spring-boot-starter-opentelemetry` |
| — | `spring-boot-starter-classic` (shim de migration : réactive l'autoconfig monolithique) |

Flyway et Liquibase exigent désormais `spring-boot-starter-flyway` / `-liquibase` explicites.
Relocalisations : `EnvironmentPostProcessor` → `org.springframework.boot`, `@EntityScan` → `org.springframework.boot.persistence.autoconfigure`, nullabilité Actuator → `org.jspecify.annotations`.
En 4.1 : `-DskipTests` **ne saute plus le traitement AOT** → utiliser `-Dmaven.test.skip=true`.

---

## 2. Jackson 3 — le piège le plus coûteux

- Le groupe/paquet passe de `com.fasterxml.jackson` à **`tools.jackson`**. **Exception : `jackson-annotations` reste sur `com.fasterxml.jackson.core`** — les imports `@JsonProperty`, `@JsonIgnore` ne changent pas.
- `ObjectMapper` (mutable) → **`JsonMapper`** (immuable, builder). Pas d'équivalent `Jackson2ObjectMapperBuilder`.
- **Défauts modifiés** :
  - `MapperFeature.SORT_PROPERTIES_ALPHABETICALLY` = **true**
  - `DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS` = **false** → les dates sortent en **ISO-8601**, plus en epoch. *C'est la régression silencieuse n°1 pour les clients mobiles existants.* Compensation temporaire : `spring.jackson.json.datetime.write-dates-as-timestamps=true`.

| Boot 3 | Boot 4 |
|---|---|
| `MappingJackson2HttpMessageConverter` | `JacksonJsonHttpMessageConverter` (`SmartHttpMessageConverter` : plus besoin de `MappingJacksonValue`) |
| `Jackson2ObjectMapperBuilderCustomizer` | `JsonMapperBuilderCustomizer` |
| `@JsonComponent` / `@JsonMixin` | `@JacksonComponent` / `@JacksonMixin` |

Échappatoire progressive : module `spring-boot-jackson2` (déprécié) + `spring.jackson.use-jackson2-defaults=true`.
Arborescence des propriétés : `spring.jackson.{serialization,deserialization,mapper,read,write,factory,datatype}.*` et `spring.jackson.json.{read,write,datetime,enum-features,json-node,constraints}.*`.

---

## 3. Versionnage d'API (natif depuis Framework 7)

```java
@RestController
@RequestMapping("/accounts/{id}")
class AccountController {
  @GetMapping                 Account any()  {}   // sans version : priorité la plus basse
  @GetMapping(version = "1.1") Account v11() {}   // match exact
  @GetMapping(version = "1.2+") Account v12() {}  // baseline : 1.2 et au-dessus
}
```
Résolution : la version déclarée la plus haute **≤** version demandée l'emporte.

```properties
spring.mvc.apiversion.use.header=X-API-Version
# alternatives : use.query-parameter, use.path-segment=<index>, use.media-type-parameter[<mediaType>]
spring.mvc.apiversion.required=false
spring.mvc.apiversion.default-version=1.0
spring.mvc.apiversion.supported=1.0,1.1,1.2
spring.mvc.apiversion.detect-supported=true
```
(Miroir WebFlux : `spring.webflux.apiversion.*`.)

- Config Java : `WebMvcConfigurer#configureApiVersioning(ApiVersionConfigurer)` → `useRequestHeader / useQueryParam / usePathSegment / useMediaTypeParameter / addSupportedVersions / setVersionRequired`.
- SPI : `ApiVersionStrategy`, `ApiVersionResolver`, `ApiVersionParser` (défaut `SemanticApiVersionParser`, `major.minor.patch`).
- **Le versionnage par segment d'URL ne se combine avec aucune autre stratégie.**
- Dépréciation : `StandardApiVersionDeprecationHandler` émet les en-têtes `Deprecation`, `Sunset`, `Link` (**RFC 9745** / **RFC 8594**).
- Exceptions : `InvalidApiVersionException`, `MissingApiVersionException`, `NotAcceptableApiVersionException` (400).
- Côté client : `.defaultVersion("1.2")` + `ApiVersionInserter.fromHeader(...)`, honoré aussi par `MockMvc`, `WebTestClient`, `RestTestClient`.

**Pièges de nommage** : la propriété est `spring.mvc.apiversion.required` (et non `api-versioning.version-required`, qui n'existe pas) ; le défaut se lie sur `default-version`.

---

## 4. Clients HTTP sortants

**`RestClient` est le choix par défaut** (bloquant, fluide). `WebClient` uniquement pour réactif/streaming. **`RestTemplate` est déprécié par la documentation en 7.0** — mais la classe n'est **pas** annotée `@Deprecated`, donc aucun avertissement de compilation : chercher les usages à la main. Pont : `RestClient.create(restTemplate)`.

### HTTP Service Clients — l'API réelle

> ⚠️ **`@HttpServiceClient` n'existe pas** (issue Framework #35244 close en `superseded`). Beaucoup d'articles et de réponses générées l'affirment : c'est faux. On utilise `@HttpExchange` + `@ImportHttpServices`.

```java
@HttpExchange(url = "/repos/{owner}/{repo}")
public interface RepositoryService {
  @GetExchange  Repository get(@PathVariable String owner, @PathVariable String repo);
  @PostExchange void create(@RequestBody NewRepo repo);
}

@SpringBootApplication
@ImportHttpServices(group = "github", types = RepositoryService.class)
class App {}
```
Sans Boot : `HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build().createClient(...)`.

```yaml
spring:
  http:
    serviceclient:
      github:                       # nom du groupe
        base-url: https://api.github.com
        connect-timeout: 2s
        read-timeout: 5s
        redirects: dont-follow
        ssl.bundle: partner
        apiversion: { default: "1.2", insert.header: X-API-Version }
    clients:                        # global — noter le PLURIEL (Boot 3 : spring.http.client.*)
      connect-timeout: 2s
      read-timeout: 5s
      cookie-handling: ...           # nouveau en 4.1
      imperative.factory: http-components   # jdk|http-components|jetty|reactor|simple
```
Détection auto (impératif) : Apache HttpClient 5 → Jetty → Reactor Netty → JDK → simple.

### SSRF — `InetAddressFilter` (nouveau en 4.1)
```java
HttpClientSettings.defaults().withInetAddressFilter(InetAddressFilter.externalAddresses());
// ou InetAddressFilter.of("10.0.0.0/8").andNot("10.0.0.5")
```
Obligatoire dès qu'une URL cible est influencée par l'appelant (proxy, webhook, gateway).

---

## 5. Null-safety JSpecify

`org.springframework.lang.@Nullable` (JSR-305) est remplacé par **JSpecify**.
```java
// package-info.java
@NullMarked
package com.example.api;
import org.jspecify.annotations.NullMarked;
```
- JSpecify annote **l'usage du type** : `@Nullable Object[]` (éléments nullables) ≠ `Object @Nullable []` (tableau nullable) ; `List<@Nullable String>`.
- **Les annotations ne sont pas héritées** : redéclarer `@Nullable` dans les overrides.
- NullAway : `NullAway:OnlyNullMarked=true`.
- Effet pratique sur les DTO : un `record` dans un paquet `@NullMarked` est non-null par contrat ; l'optionnel doit être explicite. **JSpecify est un contrat statique, Bean Validation reste le contrôle runtime des entrées non fiables — les deux, pas l'un ou l'autre.**

---

## 6. Erreurs : ProblemDetail / RFC 9457

```properties
spring.mvc.problemdetails.enabled=true
```
```java
@ExceptionHandler(OrderNotFoundException.class)
ProblemDetail handle(OrderNotFoundException ex) {
  ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  pd.setType(URI.create("https://api.example.com/errors/order-not-found"));
  pd.setTitle("Commande introuvable");
  pd.setProperty("orderId", ex.getId());
  return pd;
}
```
Abstractions : `ProblemDetail`, `ErrorResponse`, `ErrorResponseException`, `ResponseEntityExceptionHandler`. Media type `application/problem+json`. Pour surcharger le handler de Boot (ordre 0), annoter son `@ControllerAdvice` avec `@Order(-1)`.

Internationalisation via `MessageSource` — codes exacts :
```properties
problemDetail.type.<FQCN>=...
problemDetail.title.<FQCN>=...
problemDetail.<FQCN>=La méthode {0} n'est pas supportée. Méthodes autorisées : {1}
```
Côté client : `ex.getResponseBodyAs(ProblemDetail.class)`.

**Règle pour le mobile** : le `type` (URI stable) est ce sur quoi le client branche sa logique ; `detail` est du texte localisé, jamais parsé.

---

## 7. Résilience (dans le cœur de Framework 7)

Ni Spring Retry ni Resilience4j requis pour le retry.
```java
@Configuration @EnableResilientMethods class Cfg {}

@Retryable(includes = MessageDeliveryException.class, excludes = TimeoutException.class,
           maxRetries = 4, delay = 100, jitter = 10, multiplier = 2, maxDelay = 1000)
public void send() {}

@ConcurrencyLimit(10)   // borne la concurrence sortante
public void call() {}
```
- Attribut **`maxRetries`** (≠ `maxAttempts` de Spring Retry). Défauts : 3 retries, 1s, toutes exceptions. Total = `1 + maxRetries`.
- Fonctionne sur les types réactifs ; publie `MethodRetryEvent` ; programmatique via `RetryPolicy.builder()` + `RetryTemplate`.
- **Framework 7 ne fournit ni circuit breaker, ni bulkhead, ni rate limiter** → garder Resilience4j / Spring Cloud CircuitBreaker pour ceux-là.

---

## 8. Threads virtuels

```properties
spring.threads.virtual.enabled=true    # défaut : false ; Java 21+
```
C'est le bon modèle pour un backend REST I/O-bound (RestClient + JDBC) : scalabilité proche de WebFlux sans programmation réactive.
**Contrepartie : plus de pool = plus de back-pressure naturelle.** Toujours associer `@ConcurrencyLimit` + timeouts explicites sur chaque client, sinon on écroule les dépendances. Le pinning sur `synchronized` est réglé par JEP 491 (Java 24+) ; en Java 21-23 auditer les blocs `synchronized` autour d'I/O bloquantes.

---

## 9. Observabilité et Actuator

- Modules granulaires : `spring-boot-micrometer-{metrics,observation,tracing,tracing-brave,tracing-opentelemetry}`, `spring-boot-opentelemetry`.
- 4.1 : `management.opentelemetry.enabled`, `management.opentelemetry.tracing.sampler.*`, prise en charge des variables `OTEL_*`, exemplars OTLP, conventions Kafka/RabbitMQ, propagation du contexte d'observation à travers `@Async`.
- **`management.endpoint.<id>.enabled` a disparu** au profit de :
```properties
management.endpoint.<id>.access=none|read-only|unrestricted
management.endpoints.access.default=none
management.endpoints.access.max-permitted=read-only   # plafond dur
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=when-authorized
```
- Les sondes **liveness/readiness sont actives par défaut** en Boot 4.
- SSL : le statut `WILL_EXPIRE_SOON` est supprimé, remplacé par un champ `expiringChains`.

---

## 10. Tests

**Rupture majeure : `@SpringBootTest` ne fournit plus automatiquement MockMvc / WebTestClient / TestRestTemplate.** Il faut opter :
`@AutoConfigureMockMvc`, `@AutoConfigureRestTestClient`, `@AutoConfigureTestRestTemplate`, `@AutoConfigureWebServer` (4.1).

`@MockBean`/`@SpyBean` → **`@MockitoBean` / `@MockitoSpyBean`** (`org.springframework.test.context.bean.override.mockito`), utilisables uniquement sur des classes de test.

```java
@WebMvcTest(OrderController.class)
class OrderControllerTests {
  @Autowired MockMvcTester mvc;              // API AssertJ, remplace MockMvc fluent
  @MockitoBean OrderService service;

  @Test void ok() {
    assertThat(mvc.get().uri("/orders/{id}", 1))
        .hasStatusOk()
        .bodyJson().extractingPath("$.sku").isEqualTo("ABC");
  }
}
```
**`RestTestClient`** (nouveau en Framework 7) = équivalent bloquant de `WebTestClient`, remplaçant moderne de `TestRestTemplate` : `bindToServer()`, `bindToApplicationContext()`, `bindToController()`, `expectAll(...)` pour les assertions douces.

Starters de test modulaires : `spring-boot-starter-webmvc-test`, `-restclient-test`, `-webclient-test`, `-security-test`, `-actuator-test`.
Testcontainers : `@ServiceConnection` ; **Testcontainers 2.0 abandonne JUnit 4** et renomme les artefacts (`postgresql` → `testcontainers-postgresql`).
Framework 7 ajoute la mise en pause des contextes en cache (les beans `Lifecycle` sont stoppés à l'inactivité).

---

## 11. Sécurité (Spring Security 7)

DSL **lambda uniquement** : `and()` supprimé, `authorizeRequests` supprimé, `http.apply()` → **`http.with()`**.

```java
@Bean
SecurityFilterChain api(HttpSecurity http) throws Exception {
  return http
    .securityMatcher("/api/**")
    .csrf(CsrfConfigurer::disable)          // API stateless à bearer token
    .cors(Customizer.withDefaults())
    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(a -> a
        .requestMatchers("/actuator/health/**").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/**").hasAuthority("SCOPE_read")
        .anyRequest().authenticated())
    .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
    .build();
}
```
```yaml
spring.security.oauth2.resourceserver.jwt:
  issuer-uri: https://idp.example.com/issuer
  audiences: https://api.example.com
  authorities-claim-name: roles
  authority-prefix: "ROLE_"
  authorities-claim-expressions: ...      # nouveau en 4.1 (SpEL)
```
- CSRF : **désactivé** pour un token porteur (aucune credential ambiante). Pour une session par cookie (SPA/webview) : `http.csrf(csrf -> csrf.spa())` (gère XOR anti-BREACH + cookie repository).
- CORS : sans objet pour un client mobile natif ; indispensable pour navigateur/webview.
- Nouveautés 7.0 : MFA, `AuthorizationManagerFactory`, `Authentication.Builder`, encodeurs Password4j, Spring Authorization Server intégré.
- **Le rate limiting n'est PAS une fonctionnalité de Spring Security** : le faire au gateway/CDN, ou avec Bucket4j / Resilience4j `RateLimiter`, ou borner côté serveur avec `@ConcurrencyLimit`.

---

## 12. Démarrage rapide : cache AOT plutôt que natif

```shell
java -Djarmode=tools -jar app.jar extract --destination application && cd application
# Java 25+ :
java -XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh -jar app.jar   # entraînement
java -XX:AOTCache=app.aot -jar app.jar
# Java <=24 : -XX:ArchiveClassesAtExit=app.jsa puis -XX:SharedArchiveFile=app.jsa
```
Le cache est invalidé par tout changement d'application **ou** de JVM → le régénérer à chaque déploiement. L'image native (GraalVM 25+) ne se justifie que pour un plancher mémoire agressif.

---

## 13. Spécificités clients mobiles

1. **Versionner par en-tête**, jamais par segment d'URL : le binaire mobile fige sa version pour des années, l'en-tête garde URLs et clés de cache stables et se combine avec d'autres stratégies. Prévoir `required=false` + `default-version` pour les builds antérieurs au versionnage, et `version = "1.2+"` pour éviter un handler par release.
2. **ETag + requêtes conditionnelles** : le gain le plus rentable sur réseau mobile.
```java
return ResponseEntity.ok()
    .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePrivate())
    .eTag(order.version()).body(view);
// request.checkNotModified(eTag) -> 304 en GET/HEAD, 412 en écriture (concurrence optimiste gratuite)
```
Préférer un ETag explicite (colonne `version`/`updated_at`) à `ShallowEtagHeaderFilter`, qui économise la bande passante mais pas le CPU.
3. **Compression** — ne pas oublier `application/problem+json`, absent de la liste par défaut :
```properties
server.compression.enabled=true
server.compression.min-response-size=1KB
server.compression.mime-types=application/json,application/problem+json
server.http2.enabled=true
```
4. **Pagination par curseur (keyset)**, pas par offset : l'offset produit doublons et trous en scroll infini sous écritures concurrentes.
5. **Dépréciation contrôlée** : les en-têtes `Deprecation`/`Sunset` permettent au client d'afficher un « mettez à jour l'application » sans release serveur.
6. Push (FCM/APNs) est hors périmètre Spring : rien dans Boot 4 ne le couvre.

---

## Vérification avant de livrer

- [ ] Aucune référence à `@HttpServiceClient` ni à `spring.http.client.*` (singulier) ni à `spring.mvc.api-versioning.*` — ces trois n'existent pas.
- [ ] `spring-boot-starter-webmvc` (pas `-web`) ; aucun Undertow.
- [ ] Imports Jackson : `tools.jackson.*` sauf `jackson-annotations`.
- [ ] Le format de date attendu par les clients existants est vérifié après le passage à Jackson 3.
- [ ] `spring.mvc.problemdetails.enabled=true` et chaque exception métier a un `type` URI stable.
- [ ] Tous les clients HTTP sortants ont connect/read timeout ; `InetAddressFilter` si l'URL cible est influençable.
- [ ] Threads virtuels ⇒ `@ConcurrencyLimit` et pools HTTP dimensionnés.
- [ ] Tests : `@MockitoBean`, `@AutoConfigureMockMvc` explicite, `MockMvcTester`/`RestTestClient`.
- [ ] Actuator : `management.endpoints.access.default=none` + exposition explicite.
- [ ] `./mvnw verify` (ou `gradle build`) passe, y compris le traitement AOT.
