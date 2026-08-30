---
name: spring-boot-graphql-api
description: Concevoir, sécuriser, tester et exploiter une API GraphQL avec Spring for GraphQL 2.0 sur Spring Boot 4.1 (graphql-java 25). Couvre le modèle @Controller schema-first, la résolution du N+1 par DataLoader/@BatchMapping, la pagination Relay, la gestion d'erreurs, la sécurité au niveau champ, les limites de profondeur/complexité, les CVE de juin 2026, les subscriptions WebSocket/SSE, les tests GraphQlTester et l'évolution de schéma pour clients mobiles. À utiliser dès qu'il est question de GraphQL, schema.graphqls, @QueryMapping, @MutationMapping, @SchemaMapping, @BatchMapping, DataLoader, GraphiQL, subscription, federation, DGS, BFF GraphQL ou GraphQlTester.
---

# Spring for GraphQL 2.0 sur Spring Boot 4.1

> Vérifié en **août 2026**. Boot **4.1.1** → **spring-graphql 2.0.5**, **graphql-java 25.0**, Spring Framework 7.0.9.
> Matrice : spring-graphql 2.0.x ↔ Boot 4.0-4.1 ; 1.4.x ↔ Boot 3.5 (EOL OSS).

## 🔴 À faire en premier : trois CVE HIGH (10 juin 2026)

Corrigées en **2.0.4 / 1.4.6**. Épingler **≥ 2.0.4**, idéalement **2.0.5 via Boot 4.1.1**.

| CVE | Nature | Condition de déclenchement |
|---|---|---|
| **CVE-2026-41699** | Désérialisation non sûre → **RCE** | champ paginé `Connection` (désérialisation de curseur) + classes gadget au classpath |
| **CVE-2026-41700** | Cross-Site WebSocket Hijacking | transport WebSocket + auth par cookie de session + pas de contrôle d'origine au niveau WS |
| **CVE-2026-41856** | Contournement d'autorisation | `@PreAuthorize` déclaré sur une **classe parente** d'un `@Controller` silencieusement ignoré |

---

## 1. Dépendances et modules Boot 4

- Module : `org.springframework.boot:spring-boot-graphql` — package d'autoconfig **renommé** : `org.springframework.boot.autoconfigure.graphql` → **`org.springframework.boot.graphql.autoconfigure`**.
- Starter : `spring-boot-starter-graphql` (+ un starter de transport : `spring-boot-starter-webmvc`, `-webflux`, `-websocket`, `-rsocket`).
- **Piège de migration : `@GraphQlTest` n'est plus fourni par `spring-boot-starter-test`.** Ajouter **`spring-boot-starter-graphql-test`**.
- L'autoconfig est conditionnée par `@ConditionalOnGraphQlSchema` : **sans fichier `.graphqls` trouvé, GraphQL ne démarre pas silencieusement.**
- Convention de schéma : `src/main/resources/graphql/**/*.graphqls` (ou `.gqls`).

---

## 2. Propriétés `spring.graphql.*`

```properties
# ATTENTION : ce n'est PAS spring.graphql.path (Boot 2.7/3.0) mais :
spring.graphql.http.path=/graphql
spring.graphql.http.sse.timeout=5m
spring.graphql.http.sse.keep-alive=15s

spring.graphql.schema.locations=classpath:graphql/**/
spring.graphql.schema.file-extensions=.graphqls,.gqls
spring.graphql.schema.inspection.enabled=true      # rapport de cohérence code<->schéma
spring.graphql.schema.introspection.enabled=true   # PASSER À false EN PROD
spring.graphql.schema.printer.enabled=false

spring.graphql.graphiql.enabled=false              # devtools le force à true en dev
spring.graphql.graphiql.path=/graphiql

# WebSocket : AUCUN chemin par défaut, opt-in strict
spring.graphql.websocket.path=/graphql
spring.graphql.websocket.connection-init-timeout=60s
spring.graphql.websocket.keep-alive=30s

spring.graphql.cors.allowed-origins=...
spring.graphql.cors.max-age=1800s
```
Comportements HTTP : `GET /graphql` → **405** avec `Allow: POST` (les requêtes GraphQL-over-HTTP GET ne sont pas supportées) ; media type non supporté en POST → 415. La config CORS est aussi transmise au `GraphQlWebSocketHandler` pour la vérification d'origine au handshake — c'est la surface de CVE-2026-41700.

---

## 3. Modèle de contrôleur

```java
@Controller
@SchemaMapping(typeName = "Book")
class BookController {
  @QueryMapping        Book bookById(@Argument Long id) {}
  @MutationMapping     Book addBook(@Argument @Valid BookInput input, @ContextValue String userId) {}
  @SubscriptionMapping Flux<Book> newPublications() {}

  @SchemaMapping(typeName = "Book", field = "author")
  Author author(Book book) {}                      // source = objet parent

  @BatchMapping
  Mono<Map<Book, Author>> author(List<Book> books) {}   // résout le N+1

  @GraphQlExceptionHandler
  GraphQLError handle(GraphqlErrorBuilder<?> b, BindException ex) {
    return b.errorType(ErrorType.BAD_REQUEST).message(ex.getMessage()).build();
  }
}
```
Résolveurs d'arguments : `@Argument`, `@Arguments`, `ArgumentValue<T>`, `@ProjectedPayload`, objet source, `Subrange`/`ScrollSubrange`, `Sort`, `DataLoader<K,V>`, `@ContextValue`, `@LocalContextValue`, `GraphQLContext`, `Principal`, `@AuthenticationPrincipal`, `DataFetchingFieldSelectionSet`, `Locale`, `DataFetchingEnvironment`.
Retours : `T`, `Mono`, `Flux`, `suspend`/`Flow` Kotlin, `Callable`, `DataFetcherResult`.

**`ArgumentValue<T>` — distinguer « null » de « absent »**, indispensable pour les mutations de mise à jour partielle venant du mobile :
```java
@MutationMapping void updateBook(ArgumentValue<BookInput> input) {
  if (!input.isOmitted()) { /* input.value() peut être null = effacement explicite */ }
}
```
**Nullabilité (nouveau en 2.0)** : l'inspection de schéma valide la nullabilité et signale au démarrage `Book.title is NON_NULL -> 'Book#title' is NULLABLE` si votre code est annoté JSpecify (ou en Kotlin). En faire une porte de build.

---

## 4. Le N+1 : `@BatchMapping` vs `BatchLoaderRegistry`

```java
@Configuration
class BatchConfig {
  BatchConfig(BatchLoaderRegistry registry) {
    registry.forTypePair(Long.class, Author.class)
            .registerMappedBatchLoader((ids, env) -> Mono.just(loadByIds(ids)));
  }
}
// injection par type : DataLoader<Long, Author> loader
```

| | `@BatchMapping` | `BatchLoaderRegistry` + `DataLoader` |
|---|---|---|
| Clé | l'objet parent (⇒ **`equals`/`hashCode` obligatoires** — préférer les `record`) | n'importe quel type de clé |
| Réutilisable sur plusieurs champs | non | oui |
| Champ avec arguments (filtres) | ✗ — les arguments ne font pas partie de la clé | ✓ via clé composite `record FriendKey(Person p, Filter f)` |

Sémantique de retour de `@BatchMapping` : `Map<K,V>` (clé = source) **ou** `Collection<V>` **strictement dans le même ordre et de même taille** que la liste d'entrée.
**Le `DataLoaderRegistry` est créé par requête** → le cache DataLoader est per-request, aucune fuite entre requêtes, aucun cache inter-requêtes (le mettre ailleurs si besoin).

---

## 5. Pagination Relay

On n'écrit que :
```graphql
type Query { books(first: Int, after: String, last: Int, before: String): BookConnection }
```
`ConnectionTypeDefinitionConfigurer` synthétise `BookConnection`/`BookEdge`/`PageInfo` pour tout type dont le nom finit par `Connection`.

```java
@QueryMapping
Window<Book> books(ScrollSubrange subrange) {
  ScrollPosition pos = subrange.position().orElse(ScrollPosition.offset());
  int count = Math.min(subrange.count().orElse(20), 100);   // TOUJOURS plafonner
  return repository.findBy(..., q -> q.limit(count).scroll(pos));
}
```
Boot enregistre `ScrollPositionCursorStrategy` + `CursorEncoder.base64()` et les adaptateurs `Window`/`Slice`. Curseurs keyset : `JsonKeysetCursorStrategy` — sa configuration impose un `BasicPolymorphicTypeValidator` en liste blanche ; **c'est exactement la zone de CVE-2026-41699, ne jamais l'élargir**.
Intégration Spring Data : `@GraphQlRepository` + `QuerydslDataFetcher` / `QueryByExampleDataFetcher` (`.single()`, `.many()`, `.scrollable()`, `.projectAs()`). Le tri requiert un bean `SortStrategy` — **il n'y en a aucun par défaut**.

---

## 6. Erreurs

Précédence : `@GraphQlExceptionHandler` du contrôleur → `@ControllerAdvice` → beans `DataFetcherExceptionResolver` (ordonnés).
`ErrorType` : `BAD_REQUEST`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `INTERNAL_ERROR`.

**Le comportement par défaut est bon : toute exception non résolue devient un `INTERNAL_ERROR` opaque contenant l'`executionId`, journalisé en ERROR avec ce même id.** Ne pas le saboter avec un resolver fourre-tout qui renvoie `ex.getMessage()` : le protocole de support est « donnez-nous l'executionId ».

Trois familles distinctes :
- **erreurs de champ** — levées dans un DataFetcher, portent un `path`, HTTP 200 ;
- **erreurs de requête** (parsing/validation) — pas de `path`, **jamais vues par `DataFetcherExceptionResolver`** → intercepter avec un `WebGraphQlInterceptor` (`response.isValid()`, `response.transform(...)`) ;
- **erreurs de flux** de subscription → `SubscriptionExceptionResolver`.

Statut HTTP selon la négociation : avec `application/graphql-response+json` (défaut sans `Accept`), les erreurs de parsing/validation renvoient **4xx** ; avec l'ancien `application/json`, 200 + tableau `errors`.
Boot ajoute `SecurityDataFetcherExceptionResolver` : `AccessDeniedException` → `FORBIDDEN`, `AuthenticationException` → `UNAUTHORIZED`.

---

## 7. Sécurité

**Pourquoi la sécurité au niveau URL ne suffit pas** : toutes les opérations partagent une URL et une méthode. `/graphql` ne distingue pas `query { me { email } }` de `mutation { deleteAllUsers }`, et ne protège pas un champ sensible d'un type par ailleurs public. **L'autorisation doit vivre là où la donnée est récupérée.**

```java
@QueryMapping @PreAuthorize("isAuthenticated()")
User me() {}

@SchemaMapping(typeName = "User", field = "email")
@PreAuthorize("authentication.name == #user.username or hasRole('ADMIN')")
String email(User user) {}
```
Nécessite `@EnableMethodSecurity`. **Voir CVE-2026-41856 pour l'héritage de contrôleurs.**

- **Propagation vers les DataLoaders** : `SecurityContextThreadLocalAccessor` est enregistré via `ServiceLoader` (Micrometer context-propagation) → `@PreAuthorize` et l'injection de `Principal` fonctionnent dans les `@BatchMapping`. Pour d'autres thread-locals (`RequestContextHolder`), écrire son propre `ThreadLocalAccessor`.
- **Subscriptions** : `AuthenticationWebSocketInterceptor` + `AuthenticationExtractor` authentifient depuis le payload `connection_init` et propagent le `SecurityContext` à toutes les opérations du socket. **Préférer cela à l'auth par cookie sur WebSocket** (condition de CVE-2026-41700).
- **Introspection** : `spring.graphql.schema.introspection.enabled=false` appelle `Introspection.enabledJvmWide(false)` — **effet JVM entier, non paramétrable par endpoint**. Désactiver aussi `graphiql` et `schema.printer` en prod. graphql-java 25 applique de toute façon `GoodFaithIntrospection` (max 500 champs, profondeur 20).
- **Limites d'abus — rien n'est activé par défaut au-delà des limites du parser.** Tout bean `Instrumentation` est repris par Boot :
```java
@Bean Instrumentation maxDepth()      { return new MaxQueryDepthInstrumentation(10); }
@Bean Instrumentation maxComplexity() { return new MaxQueryComplexityInstrumentation(200); }
```
Limites parser par défaut (graphql-java 25) : 1 048 576 caractères, 15 000 tokens, profondeur de règle 500.
- **Batching de requêtes** : spring-graphql n'implémente **pas** le batch en tableau `[{query},{query}]` — un HTTP = une opération, vecteur d'amplification supprimé. Reste l'amplification par alias, d'où l'instrumentation de complexité. Le rate limiting se place dans un `WebGraphQlInterceptor` (il voit les en-têtes HTTP et peut court-circuiter) ou au gateway.
- **Persisted / trusted documents** : ⚠️ **aucun support natif en spring-graphql 2.0**, aucune propriété `spring.graphql.*`. Les classes existent une couche en dessous (`graphql.execution.preparsed.persisted.*`) et se câbleraient via `GraphQlSourceBuilderCustomizer` → `configureGraphQl(...)`, mais ce montage n'est pas documenté ni validé. **Options défendables : le faire au gateway (safelisting), ou un `WebGraphQlInterceptor` maison qui échange un hash contre un document stocké.**

---

## 8. Subscriptions

| Transport | Handler | Notes |
|---|---|---|
| WebSocket | `GraphQlWebSocketHandler` | protocole `graphql-ws` ; porte aussi queries/mutations |
| SSE | `GraphQlSseHandler` | POST + `Accept: text/event-stream` sur **le même** `spring.graphql.http.path` ; mode « connexions distinctes » uniquement ; subscriptions seulement |
| RSocket | `GraphQlRSocketHandler` | request-response / request-stream |

`WebSocketGraphQlInterceptor` : callbacks connexion/annulation, **au plus un par chaîne**.
Back-pressure : de bout en bout en WebFlux ; en MVC/SSE, borner explicitement le `Flux` (`limitRate`, `onBackpressureBuffer`).

---

## 9. Tests

```java
@GraphQlTest(BookController.class)          // slice : pas de couche web
class BookControllerTests {
  @Autowired GraphQlTester tester;
  @MockitoBean BookService service;

  @Test void bookById() {
    tester.documentName("bookDetails")      // src/test/resources/graphql-test/bookDetails.graphql
          .variable("id", "book-1")
          .execute()
          .path("bookById.name").entity(String.class).isEqualTo("1984");
  }
}
```
`@GraphQlTest` ne charge **que** : `@Controller`, `@ControllerAdvice`, `RuntimeWiringConfigurer`, `@JacksonComponent`, `Converter`, `DataFetcherExceptionResolver`, `Instrumentation`, `GraphQlSourceBuilderCustomizer`. Pas de `@Service`/`@Repository`.

| Tester | Portée |
|---|---|
| `HttpGraphQlTester` | via `WebTestClient`, serveur réel ou contexte |
| `WebGraphQlTester` | serveur, **à travers la chaîne d'interceptors** (tester en-têtes/interceptors sans serveur) |
| `ExecutionGraphQlServiceTester` | sous la couche web |
| `WebSocketGraphQlTester` / `RSocketGraphQlTester` | subscriptions |

Full-stack : `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@AutoConfigureHttpGraphQlTester`.
Subscriptions : `.executeSubscription().toFlux("greetings", String.class)` + `StepVerifier` — **ne fonctionne pas** avec `HttpGraphQlTester` lié à MockMvc.

---

## 10. Observabilité

| Observation | Clés basse cardinalité | Haute cardinalité |
|---|---|---|
| `graphql.request` | `graphql.operation.type`, `graphql.outcome` | `graphql.execution.id`, `graphql.operation.name` |
| `graphql.datafetcher` | `graphql.error.type`, `graphql.field.name`, `graphql.outcome` | `graphql.field.path` |
| `graphql.dataloader` | `graphql.error.type`, `graphql.loader.name`, `graphql.outcome` | **`graphql.loader.size`** |

Seuls les data fetchers **non triviaux** sont instrumentés (l'accès POJO est ignoré), ce qui garde le nombre de spans raisonnable.
**`graphql.loader.size` est le meilleur signal de régression N+1 en production : un loader dont la taille vaut systématiquement 1 signifie que le batching ne se fait pas.**

---

## 11. Federation, DGS, position schema-first

- **Apollo Federation** via `federation-jvm` : bean `FederationSchemaFactory` + `GraphQlSourceBuilderCustomizer` → `builder.schemaFactory(f::createGraphQLSchema)` ; côté contrôleur, **`@EntityMapping`** (formes unitaire, batch ordonnée, et DataLoader).
- **DGS et Spring for GraphQL n'ont pas fusionné en un seul projet.** DGS s'appuie désormais sur Spring for GraphQL en interne (`graphql-dgs-spring-graphql-starter`). Consigne : **ne pas mélanger `@DgsQuery`/`@DgsComponent` et `@QueryMapping` dans une même base de code**. Dans l'autre sens, `DgsGraphQlClient` (client typé via DGS Codegen) est disponible en 2.0.
- **Le framework est schema-first, sans ambiguïté** : l'autoconfig est conditionnée à la présence du SDL, `SchemaMappingInspector` vérifie le code contre le schéma, et il n'existe aucun générateur code-first dans spring-graphql.
- **Pas d'upload multipart** : la doc renvoie explicitement vers les URL pré-signées (ou `nkonev/multipart-spring-graphql`).

---

## 12. Côté client / BFF

`HttpSyncGraphQlClient` (bloquant, `RestClient`), `HttpGraphQlClient` (`WebClient`, SSE), `WebSocketGraphQlClient`, `RSocketGraphQlClient`, `DgsGraphQlClient`.
```java
client.documentName("projectReleases").variable("slug", "spring-framework")
      .retrieve("project").toEntity(Project.class);
```
Exceptions : `FieldAccessException`, `GraphQlClientException`, `SubscriptionErrorException`, `GraphQlTransportException`.
`ArgumentValue` côté client nécessite le bean `GraphQlJacksonModule` (Jackson 3) ou `GraphQlJackson2Module`.
**BFF** : `HttpSyncGraphQlClient` + threads virtuels est le montage le plus simple en Boot 4.1. Un `WebSocketGraphQlClient` **par serveur amont** (il multiplexe), jamais un par requête.

---

## 13. Threads virtuels, AOT, natif

**`spring.threads.virtual.enabled=true` change le comportement de GraphQL** : `AnnotatedControllerConfigurer` reçoit l'`applicationTaskExecutor`, donc les méthodes de contrôleur **bloquantes sont invoquées sur des threads virtuels** → les champs frères d'un même niveau se résolvent en parallèle sans écrire une seule ligne de réactif. Deux conséquences :
- `@Transactional` sur une méthode de contrôleur = **une transaction par résolution de champ**, pas par requête (c'est la recommandation de la doc) ;
- tout bloc `synchronized` dans un fetcher épingle un carrier thread (réglé par JEP 491 / Java 24+).

AOT / natif :
- Les hints de ressources couvrent **exactement** `graphql/**/*.graphqls` et `*.gqls`. **Si vous changez `schema.locations` ou `file-extensions`, le schéma est introuvable en image native** sans `RuntimeHintsRegistrar` maison.
- Les data fetchers déclarés manuellement via `RuntimeWiringConfigurer` et les types de réponse de `GraphQlClient` **ne sont pas découverts** → `@RegisterReflectionForBinding(...)`.

---

## 14. Conception pour clients mobiles

1. **Ne pas versionner : faire évoluer.** Une app mobile vit des années sur le terrain. Ajouts additifs, directive `@deprecated(reason: "...")`, jamais de changement de sens d'un champ existant. Diffusion du schéma imprimé en CI (`schema.printer.enabled=true` en environnement de test) pour diffuser les ruptures.
2. **Retirer un champ déprécié seulement quand la télémétrie le permet** : tag `graphql.operation.name` + en-tête de version client injecté via un interceptor et lu en `@ContextValue`.
3. **Nullabilité : être permissif sur les champs de sortie, strict sur les arguments et les inputs.** En GraphQL, un champ non-null qui échoue **propage le null vers le parent** et peut vider tout un écran. `ID!` sur les arguments, nullable par défaut sur les sorties sauf impossibilité réelle.
4. **Réduire le payload : trusted documents** (manifeste d'opérations autorisées construit au build). Le client n'envoie qu'un identifiant : corps réduits, moins d'aller-retours à froid, et surtout **rejet des requêtes arbitraires**, ce qui règle profondeur/complexité et permet de couper l'introspection. Support natif absent (§7) → gateway ou interceptor.
5. **Limiter le sur-fetch côté serveur** : `DataFetchingFieldSelectionSet` pour restreindre la projection SQL aux champs réellement sélectionnés, en complément des projections Spring Data.
6. **Subscriptions : SSE sur HTTP/2 plutôt que WebSocket** par défaut — traverse mieux les proxys, reconnexion native, et évite entièrement la classe de faille de hijacking WS par cookie. Régler `spring.graphql.http.sse.keep-alive` pour survivre aux NAT mobiles.
7. **Curseurs opaques base64 uniquement**, jamais d'offset exposé, `first`/`last` plafonnés côté serveur.

---

## Vérification avant de livrer

- [ ] spring-graphql **≥ 2.0.4** (Boot ≥ 4.1.0) — les trois CVE de juin 2026.
- [ ] `spring-boot-starter-graphql-test` présent si des `@GraphQlTest` existent.
- [ ] Prod : `introspection.enabled=false`, `graphiql.enabled=false`, `schema.printer.enabled=false`.
- [ ] Beans `MaxQueryDepthInstrumentation` et `MaxQueryComplexityInstrumentation` déclarés.
- [ ] Chaque relation to-one/to-many a un `@BatchMapping` ou un `DataLoader` ; vérifié par `graphql.loader.size > 1`.
- [ ] `@PreAuthorize` sur les champs sensibles, pas seulement sur `/graphql` ; pas d'annotation d'autorisation héritée d'une classe parente sans vérification.
- [ ] WebSocket : auth par `connection_init`, jamais par cookie ; origines CORS explicites.
- [ ] Pagination : `first`/`last` plafonnés ; validateur polymorphique du curseur keyset non élargi.
- [ ] Rapport d'inspection de schéma sans `NullnessError` au démarrage.
- [ ] `@MockitoBean` (et non `@MockBean`) dans les tests.
