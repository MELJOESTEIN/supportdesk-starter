---
name: spring-boot-soap-webservices
description: Produire et consommer des services SOAP avec Spring Boot 4.1 et Spring Web Services 5.0, ou décider quand utiliser Apache CXF à la place. Couvre l'approche contract-first XSD/WSDL, la génération JAXB sur Java 21/25, les endpoints @Endpoint, les SOAP faults, la validation XSD, WS-Security avec WSS4J 4, le client WebServiceTemplate avec les nouveaux HttpClientSettings de Boot 4, les slices de test, le durcissement XML (XXE, billion laughs) et les patterns de modernisation façade REST/GraphQL pour clients web et mobiles. À utiliser dès qu'il est question de SOAP, WSDL, XSD, JAXB, xjc, wsimport, @Endpoint, @PayloadRoot, WebServiceTemplate, WS-Security, MTOM, SAAJ, CXF, ou d'exposer/consommer un backend legacy XML.
---

# SOAP avec Spring Boot 4.1 / Spring WS 5.0

> Vérifié en **août 2026**. **Spring WS 5.0.2** est géré par le BOM **Boot 4.1.1** (Spring Framework 7.0.9).
> **Spring WS est bien compatible Boot 4 / Spring 7** — il n'y a aucune raison de rester en 3.5, d'autant que **le support OSS de Boot 3.5 s'est arrêté le 30/06/2026**.

## Matrice de versions

| Composant | Version |
|---|---|
| Spring Web Services | **5.0.2** (JDK 17-27, Spring Framework 7, Spring Security 7, Jakarta EE 11, WSS4J 4.0, JUnit 6, JSpecify) |
| WSS4J / Santuario xmlsec | 4.0.1 / 4.0.4 |
| `jakarta.xml.bind-api` / `jaxb-runtime` | 4.0.5 / **4.0.9** (il n'existe pas de 5.x) |
| `jakarta.xml.soap-api` / `saaj-impl` | 3.0.2 / 3.0.6 |
| `jakarta.xml.ws-api` | 4.0.3 |
| Apache CXF | **4.2.3** (baseline JDK 17, Jakarta EE 11, Spring 7 / Boot 4 ; pas de CXF 5.x) |
| `wsdl4j` | 1.6.3 — dernière release **2017**, toujours la seule option |

---

## 1. Dépendances

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-webservices</artifactId>  <!-- RENOMMÉ : plus de tiret -->
</dependency>
```
L'ancien `spring-boot-starter-web-services` existe encore mais est **déprécié**. Le starter tire `spring-boot-starter-webmvc`, `spring-boot-webservices`, `saaj-impl`, `jakarta.xml.ws-api`. `spring-ws-core` apporte transitivement l'API JAXB + `jaxb-runtime` + l'API SAAJ. **`wsdl4j` et `httpclient5` sont à ajouter soi-même.**

### JAXB/JAX-WS retirés du JDK
JEP 320 (Java 11) a supprimé `java.xml.bind`, `java.xml.ws` et les outils `xjc`/`wsimport`/`wsgen`/`schemagen`. Sur Java 21/25, **tout est une dépendance explicite**. Migration de namespace : `javax.xml.bind|soap|ws|jws.*` → `jakarta.*` ; **les sources générées par un XJC 2.x ne compilent pas — il faut régénérer.**

### Ruptures Spring WS 4.x → 5.0

| Élément | Statut |
|---|---|
| **`WsConfigurerAdapter`** | **supprimé** en 5.0 → implémenter `WsConfigurer` (toutes les méthodes sont `default`) |
| **XWS-Security** (`...soap.security.xwss`, `XwsSecurityInterceptor`) | **supprimé** depuis la 4.0 — pas déprécié, absent. Migrer vers `Wss4jSecurityInterceptor` |
| XMLUnit 1 | supprimé ; package `...test.support.matcher.xmlunit2` → **`...matcher.xmlunit`** |
| Commons HttpClient 3, EhCache 2 | supprimés en 4.1 |
| Apache Axiom | **restauré** en 4.1 (version Jakarta) |
| OXM | seul **`Jaxb2Marshaller`** reste pertinent (Castor, XMLBeans, JiBX partis) |

---

## 2. Contract-first : XSD → WSDL → endpoint

**En Boot 4, `@EnableWs` et l'enregistrement manuel de `MessageDispatcherServlet` ne sont plus nécessaires** — l'autoconfiguration s'en charge (le guide officiel les a retirés).

```properties
spring.webservices.path=/services                        # défaut
spring.webservices.wsdl-locations=classpath:META-INF/schemas/
spring.webservices.servlet.init.transformWsdlLocations=true
```
Boot crée automatiquement un `SimpleXsdSchema` par `*.xsd` et un `SimpleWsdl11Definition` par `*.wsdl` trouvés sous `wsdl-locations`, nommés d'après le fichier.

### WSDL dynamique
```java
@Bean(name = "countries")                    // nom du bean => /services/countries.wsdl
DefaultWsdl11Definition countries(XsdSchema countriesSchema) {
  DefaultWsdl11Definition d = new DefaultWsdl11Definition();
  d.setPortTypeName("CountriesPort");        // obligatoire
  d.setLocationUri("/services");
  d.setTargetNamespace("https://example.com/countries");
  d.setSchema(countriesSchema);
  return d;
}
@Bean XsdSchema countriesSchema() { return new SimpleXsdSchema(new ClassPathResource("countries.xsd")); }
```
Défauts : suffixes `Request`/`Response`/`Fault`, **`createSoap11Binding=true`**, **`createSoap12Binding=false`**, `serviceName = portTypeName + "Service"`.
XSD avec `import`/`include` → `CommonsXsdSchemaCollection` (WS-Commons XmlSchema), option `setInline(true)`.

### Quand publier un WSDL statique plutôt que généré
- Le WSDL est un **contrat négocié** qui doit rester **stable octet pour octet** entre versions.
- Constructions que `DefaultWsdl11Definition` ne sait pas émettre : RPC/encoded, plusieurs portTypes/bindings/services, WS-Policy, bindings WS-Addressing, en-têtes SOAP personnalisés, chaînes de `wsdl:import`.
- Vous êtes **le côté serveur d'un contrat écrit par quelqu'un d'autre** (cas brownfield typique).

Interception / personnalisation :
```java
@Configuration
class WsCustomizer implements WsConfigurer {          // PAS extends WsConfigurerAdapter
  @Override public void addInterceptors(List<EndpointInterceptor> interceptors) { ... }
}
```

---

## 3. Génération de code sur Java 21/25

### XSD → Java (XJC)
| Outil | Version | Usage |
|---|---|---|
| `org.codehaus.mojo:jaxb2-maven-plugin` | **4.1.0** | défaut Maven, goal `xjc` |
| `org.jvnet.jaxb:jaxb-maven-plugin` | 4.0.16 | si vous avez besoin d'épisodes, catalogues, plugins JAXB (`-Xannotate`, `-Xfluent-api`) — successeur de `maven-jaxb2-plugin` (GA mort) |
| Gradle : `com.github.bjornvester.xjc` | 1.9.1 | maintenu |

### WSDL → Java (remplaçants de `wsimport`)
| Outil | Version | Quand |
|---|---|---|
| `com.sun.xml.ws:jaxws-maven-plugin` | 4.0.5 | portage le plus direct d'un build `wsimport` legacy |
| `org.apache.cxf:cxf-codegen-plugin` | 4.2.3 | WSDL WS-*, WS-Policy, options wrapper/async |
| Gradle : `io.mateo:cxf-codegen-gradle` | 3.0.0 | **publié sur Maven Central, plus sur le Plugin Portal** (portail figé en 1.2.1/2023) |
| `com.github.bjornvester.wsdl2java` | 2.0.2 (2023) | **obsolète, à éviter** |

**En contract-first Spring WS, on ne génère normalement pas depuis le WSDL** : on génère les classes JAXB depuis le XSD et on laisse `DefaultWsdl11Definition` produire le WSDL. WSDL→Java est un sujet **client** (ou CXF).

---

## 4. Endpoints

```java
@Endpoint
public class CountryEndpoint {
  private static final String NS = "https://example.com/countries";

  @PayloadRoot(namespace = NS, localPart = "getCountryRequest")
  @ResponsePayload
  public GetCountryResponse getCountry(@RequestPayload GetCountryRequest request) { ... }

  @SoapAction("http://example.com/RequestOrders")     // routage par SOAPAction
  @ResponsePayload
  public OrdersResponse orders(@RequestPayload OrdersRequest r) { ... }

  @Namespaces(@Namespace(prefix = "s", uri = NS))
  @PayloadRoot(namespace = NS, localPart = "flightRequest")
  public void byXPath(@XPathParam("/s:flightRequest/@id") String id, MessageContext ctx) { ... }
}
```
Processeurs disponibles : `MarshallingPayloadMethodProcessor` (POJO/JAXB), `SourcePayloadMethodProcessor`, **`StaxPayloadMethodArgumentResolver`** (`XMLStreamReader` — pour très gros payloads), `XPathParamMethodArgumentResolver`, `MessageContextMethodArgumentResolver`.

```java
@Bean Jaxb2Marshaller marshaller() {
  Jaxb2Marshaller m = new Jaxb2Marshaller();
  m.setContextPath("com.example.gen");   // préférer à setPackagesToScan (lent, chargement eager)
  return m;                              // laisser processExternalEntities=false et supportDtd=false
}
```
`MarshallingPayloadMethodProcessor` est enregistré automatiquement dès qu'il existe **une seule** paire `Marshaller`/`Unmarshaller`.

---

## 5. SOAP faults

```java
@Bean
SoapFaultMappingExceptionResolver exceptionResolver() {
  SoapFaultMappingExceptionResolver r = new SoapFaultMappingExceptionResolver();
  Properties m = new Properties();
  m.setProperty(Exception.class.getName(), "SERVER");
  m.setProperty(ValidationFailureException.class.getName(), "CLIENT,Requête invalide");
  m.setProperty(UnknownCountryException.class.getName(),
      "{https://example.com/countries}CountryNotFound,Pays inconnu");
  r.setExceptionMappings(m);
  r.setOrder(Ordered.HIGHEST_PRECEDENCE);
  return r;
}
```
Format de `SoapFaultDefinitionEditor` : `faultCode,faultStringOrReason,locale`, où `faultCode` est un `QName` ou l'une des constantes **`SERVER`/`RECEIVER`** et **`CLIENT`/`SENDER`**.
Forme annotée : `@SoapFault(faultCode = FaultCode.CLIENT, faultStringOrReason = "...")` sur la classe d'exception, résolue par `SoapFaultAnnotationExceptionResolver`.
Détail métier : surcharger `customizeFault(endpoint, ex, fault)` et `fault.addFaultDetail().addFaultDetailElement(qname).addText(...)`.
**SOAP 1.1 vs 1.2** : `SERVER`/`CLIENT` sont les codes 1.1, `RECEIVER`/`SENDER` ceux de 1.2 ; Spring WS traduit selon la `SoapVersion` active. Choix : `SaajSoapMessageFactory.setSoapVersion(SoapVersion.SOAP_12)` + `setCreateSoap12Binding(true)`. SOAP 1.2 utilise `application/soap+xml` avec un paramètre `action`, pas d'en-tête `SOAPAction`.

---

## 6. Validation et traçage

```java
PayloadValidatingInterceptor v = new PayloadValidatingInterceptor();
v.setSchema(new ClassPathResource("countries.xsd"));
v.setValidateRequest(true);
v.setValidateResponse(true);        // à couper sur les chemins chauds si la réponse est sûre par construction
v.setAddValidationErrorDetail(true);
```
Une requête invalide devient un fault **Client/Sender** avec des éléments `<spring-ws:ValidationError>` en détail.

Traçage intégré (aucun interceptor requis) :
```properties
logging.level.org.springframework.ws.server.MessageTracing=DEBUG   # racine du payload
logging.level.org.springframework.ws.client.MessageTracing.sent=TRACE  # message entier
```
> **`TRACE` journalise les enveloppes complètes, mots de passe `UsernameToken` et données personnelles compris. Jamais en production sans rédaction.**

---

## 7. WS-Security (WSS4J 4)

Dépendance `org.springframework.ws:spring-ws-security` → `Wss4jSecurityInterceptor` (`...soap.security.wss4j2`). **`wss4j.version` n'est PAS géré par le BOM Boot** : la version arrive transitivement, la pinner soi-même pour patcher indépendamment.

```java
Wss4jSecurityInterceptor i = new Wss4jSecurityInterceptor();
i.setValidationActions("Timestamp UsernameToken");
i.setValidationCallbackHandler(handler);
i.setValidationTimeToLive(300);
i.setTimestampStrict(true);                 // défaut false — À ACTIVER
i.setValidationReplayCache(replayCache);    // OBLIGATOIRE : sans lui, aucune protection anti-rejeu
i.setSecurementActions("Timestamp Signature");
i.setSecurementSignatureAlgorithm(WSS4JConstants.RSA_SHA256);   // ne pas laisser RSA-SHA1
i.setSecurementSignatureDigestAlgorithm(WSS4JConstants.SHA256);
i.setAllowRSA15KeyTransportAlgorithm(false);  // défaut false — le garder (Bleichenbacher)
i.setBspCompliant(true);
i.setEnableRevocation(true);
i.setValidationSubjectDnConstraints(List.of(Pattern.compile(".*CN=partner\\.example\\.com.*")));
```
Actions de securement : `UsernameToken`, `UsernameTokenSignature`, `Timestamp`, `Encrypt`, `Signature`, `NoSecurity`.
Handlers : `KeyStoreCallbackHandler`, `SimplePasswordValidationCallbackHandler`, `SpringSecurityPasswordValidationCallbackHandler` ; supports : `CryptoFactoryBean`, `KeyStoreFactoryBean`, caches de rejeu dans `...wss4j2.cache`. Nouveau en 4.1 : `setAttachmentCallbackHandler` (signer/chiffrer les pièces jointes), `setUseSingleCertificate` configurable.

### Recommandation 2026
**Privilégier TLS 1.3 + mTLS au transport ; réserver WS-Security aux cas où un contrat ou un intermédiaire l'impose réellement** (non-répudiation par signature XML, confidentialité de bout en bout à travers un ESB, spec partenaire exigeant `UsernameToken`). La cryptographie XML au niveau message est coûteuse (canonicalisation + DOM) et historiquement riche en CVE (XML Signature Wrapping, oracle sur `Encrypt`, RSA-1.5). En Boot 4, mTLS tient en un `SslBundle` :
```properties
spring.ssl.bundle.jks.partner.keystore.location=classpath:client.p12
spring.ssl.bundle.jks.partner.truststore.location=classpath:truststore.p12
server.ssl.bundle=partner
server.ssl.client-auth=need
```

---

## 8. Client — ce qui a changé en Boot 4

**`WebServiceTemplateBuilder.setConnectTimeout(...)` / `setReadTimeout(...)` ont été supprimés.** Nouvelle forme :
```java
@Bean
WebServiceTemplate webServiceTemplate(WebServiceTemplateBuilder builder, Jaxb2Marshaller m, SslBundle ssl) {
  HttpClientSettings settings = HttpClientSettings.defaults()
      .withConnectTimeout(Duration.ofSeconds(2))
      .withReadTimeout(Duration.ofSeconds(10))
      .withRedirects(HttpRedirects.DONT_FOLLOW)
      .withSslBundle(ssl)
      .withInetAddressFilter(InetAddressFilter.externalAddresses());  // 4.1 : anti-SSRF
  return builder
      .httpMessageSenderFactory(WebServiceMessageSenderFactory.http(
          ClientHttpRequestFactoryBuilder.httpComponents(), settings))
      .setDefaultUri("https://partner.example.com/services")
      .setMarshaller(m).setUnmarshaller(m)
      .setCheckConnectionForFault(true)
      .build();
}
```
**Quel message sender ?** Laisser Boot choisir : `WebServiceMessageSenderFactory.http(...)` renvoie un `ClientHttpRequestMessageSender` sur `ClientHttpRequestFactoryBuilder.detect()` (ordre : HttpComponents 5 → Jetty → Reactor Netty → JDK → simple). Comme ni `spring-ws-core` ni le starter n'apportent httpclient5, **un client SOAP Boot 4 par défaut utilise le `HttpClient` du JDK**. Ajouter `httpclient5` pour le **pooling de connexions, les limites par route, le keep-alive, NTLM/Kerberos, l'auth préemptive** — le choix habituel en production.
> `HttpComponents5MessageSender` court-circuite l'abstraction Spring et **n'est pas instrumenté** par Micrometer : raison de plus de préférer le sender détecté par Boot.

```java
public class CountryClient extends WebServiceGatewaySupport {
  public GetCountryResponse get(String name) {
    return (GetCountryResponse) getWebServiceTemplate()
        .marshalSendAndReceive(req, new SoapActionCallback("http://example.com/GetCountry"));
  }
}
```
WS-Addressing : `ActionCallback`. Streaming de réponse : `WebServiceMessageExtractor`.
**MTOM** : `Jaxb2Marshaller.setMtomEnabled(true)` + champ `DataHandler`/`byte[]` annoté `@XmlMimeType`, avec une factory `MimeMessage`. L'ergonomie MTOM de Spring WS est notoirement médiocre (spring-ws#982 ouvert) — **si MTOM est central, utiliser CXF.**

---

## 9. Tests

```java
@WebServiceServerTest(CountryEndpoint.class)   // package Boot 4 : org.springframework.boot.webservices.test.autoconfigure.server
class CountryEndpointTests {
  @Autowired MockWebServiceClient client;
  @MockitoBean CountryRepository repo;

  @Test void ok() {
    client.sendRequest(withPayload(new StringSource("<getCountryRequest .../>")))
          .andExpect(noFault())
          .andExpect(validPayload(new ClassPathResource("countries.xsd")))
          .andExpect(xpath("//c:country/c:capital", Map.of("c", NS)).evaluatesTo("Madrid"));
  }
}
```
Le scan de `@WebServiceServerTest` est restreint aux beans `@Endpoint` et `EndpointInterceptor`. Matchers : `noFault()`, `payload(...)`, `validPayload(...)`, `xpath(...)`, `soapHeader(QName)`, `clientOrSenderFault(...)`, `serverOrReceiverFault(...)`, `mustUnderstandFault()`.

```java
@WebServiceClientTest(CountryClient.class)
class CountryClientTests {
  @Autowired MockWebServiceServer server;  @Autowired CountryClient client;
  @Test void ok() {
    server.expect(connectionTo("https://partner/ws"))
          .andExpect(payload(new StringSource("<getCountryRequest .../>")))
          .andRespond(withPayload(new ClassPathResource("country-response.xml")));
    client.get("Spain");
    server.verify();
  }
}
```
Si le bean injecte un `WebServiceTemplate` (et non le builder) : `@AutoConfigureWebServiceClient(registerWebServiceTemplate = true)`.
La comparaison est un **diff XMLUnit 2** (sensible aux namespaces, insensible à l'ordre des attributs), pas une égalité de chaînes. Utiliser `xmlunit-placeholders` (`${xmlunit.isNumber}`, `${xmlunit.ignore}`) pour les timestamps et UUID.

---

## 10. Apache CXF — quand le préférer

```xml
<dependency>
  <groupId>org.apache.cxf</groupId>
  <artifactId>cxf-spring-boot-starter-jaxws</artifactId>
  <version>4.2.3</version>   <!-- NON géré par le BOM Boot : pinner -->
</dependency>
```
**CXF** si : pile WS-* (WS-Addressing, WS-Policy, WS-SecurityPolicy, WS-ReliableMessaging, SAML/STS), **MTOM/XOP**, JAX-WS code-first (`@WebService`) ou génération client `wsdl2java`, WSDL tiers hostile (RPC/encoded, bindings multiples), `Provider`/`Dispatch` et handler chains. Bonus : `cxf-rt-features-metrics` (Micrometer) inclus. Note : `LoggingFeature` supprimé en 4.2.
**Spring WS** si : contract-first doc/literal, le XSD est le contrat, modèle `@Endpoint` idiomatique Spring, empreinte réduite, autoconfiguration et slices de test Boot. Spring WS n'est délibérément **pas** une implémentation JAX-WS.

---

## 11. Modernisation et interop (web / mobile)

- **Façade SOAP→REST (couche anticorruption).** Un service Boot 4 devant le backend legacy : `WebServiceTemplate` en sortie, `@RestController` + Jackson (ou une couche GraphQL) en entrée. **Mapper les DTO JAXB vers des records de domaine à la frontière — ne jamais laisser fuiter les types générés dans l'API publique**, sinon on republie le contrat SOAP habillé en JSON.
- **Strangler fig.** Router par opération au gateway ; chaque opération réimplémentée bascule sa route de la façade vers le nouveau service. Garder l'endpoint SOAP stable jusqu'à trafic nul — mesuré par opération, pas supposé.
- **Ne jamais faire parler SOAP à un client mobile ou navigateur.** Coûts concrets : enveloppe XML + déclarations de namespaces + en-têtes WS-Security gonflent le payload d'un facteur **3 à 8** par rapport au JSON équivalent ; le parsing DOM (SAAJ) alloue un graphe complet par message sur téléphone ; il n'existe pas de client SOAP utilisable dans les chaînes d'outils mobiles/web modernes ; WS-Security impliquerait de livrer des keystores sur un appareil non fiable ; et chaque évolution de schéma impose une release client coordonnée. **Terminer SOAP côté serveur.**
- **BFF** : un backend-for-frontend par classe de client, agrégeant plusieurs opérations SOAP en une réponse JSON, et portant le cache que le backend legacy ne sait pas faire. C'est aussi là que `withInetAddressFilter(...)` doit vivre si un endpoint est influencé par l'appelant.
- **Tester le joint par contrat** : rejouer en CI des réponses réelles enregistrées via `MockWebServiceServer` — c'est ce qui empêche un changement silencieux de WSDL de devenir un incident.

---

## 12. Observabilité, threads virtuels, natif, performance

- **Image native : non.** Le wiki Spring Boot/GraalVM est explicite — *Spring WS ne supporte ni AOT ni GraalVM Native*. L'issue spring-ws#1329 est ouverte sans engagement depuis 2023. Causes : `JAXBContext` construit son modèle par réflexion à l'exécution, SAAJ/`DocumentBuilderFactory`/`TransformerFactory` passent par `ServiceLoader`, `wsdl4j` fait de la résolution réflexive. **Si vous avez besoin de natif : CXF, ou pas de SOAP dans ce service.** (Avec `jaxb-runtime` au classpath, le build natif exige `libfreetype` sur Linux/macOS.)
- **Threads virtuels** : `MessageDispatcherServlet` est une servlet bloquante classique, donc `spring.threads.virtual.enabled=true` s'applique comme pour MVC — le bon réglage pour une façade SOAP I/O-bound. Deux réserves : le pinning sur `synchronized` n'est réglé que par **JEP 491 / Java 24+** alors que JAXB, SAAJ et WSS4J en usent en chemin chaud (**viser Java 25**) ; et le `PoolingHttpClientConnectionManager` d'HttpClient 5 devient le goulot (**2 connexions par route par défaut !**) — relever `maxTotal`/`defaultMaxPerRoute` via `PoolingHttpClientConnectionManagerBuilderCustomizer`.
- **Marshaller : réutiliser, pas pooler.** `JAXBContext` est coûteux à construire et **thread-safe** ; `Marshaller`/`Unmarshaller` sont bon marché et **non** thread-safe. `Jaxb2Marshaller` détient un `JAXBContext` et crée un marshaller par appel → **le déclarer en bean singleton, jamais par requête**.
- **Gros payloads** : `AxiomSoapMessageFactory` (StAX, `setPayloadCaching(false)`) plutôt que SAAJ (DOM en mémoire), ou consommer un `XMLStreamReader`. Attention : la signature/chiffrement WS-Security force la matérialisation — streaming et WSS4J ne se composent pas.
- **Observabilité** : le serveur est instrumenté au niveau servlet, donc **toutes les opérations tombent dans un seul URI `/services`**. Ajouter un `EndpointInterceptor` qui tague l'observation avec le local-part de la racine du payload ou la SOAPAction, sinon aucune visibilité par opération.

---

## 13. Durcissement XML — la vraie surface d'attaque

**1. XXE.** `Jaxb2Marshaller` est **sûr par défaut** en Spring 7 : `processExternalEntities=false`, `supportDtd=false` (et activer le premier réactive implicitement le second). **Ne pas les basculer.** Pour toute factory créée à la main :
```java
dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);  // contrôle le plus fort
dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
dbf.setXIncludeAware(false); dbf.setExpandEntityReferences(false);
xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
```
**2. Billion laughs / DoS.** `disallow-doctype-decl=true` tue les bombes d'expansion d'entités. Limites secondaires : `jdk.xml.entityExpansionLimit`, `totalEntitySizeLimit`, `elementAttributeLimit`, `maxOccurLimit`, `maxElementDepth`. Plafonner la taille de corps au reverse proxy ou via un `Filter` — `server.tomcat.max-http-form-post-size` **ne couvre pas** les corps SOAP.
**3. La validation XSD est un contrôle de sécurité**, pas un confort : elle rejette les payloads structurellement hostiles avant le marshaller. Borner le schéma avec `maxLength`, `maxOccurs`, `pattern` sur chaque chaîne et chaque répétition — un `maxOccurs="unbounded"` de chaînes est une allocation non bornée. En externe, envisager `setAddValidationErrorDetail(false)` pour ne pas divulguer la structure du schéma.
**4. WS-Security** : `bspCompliant=true`, `allowRSA15KeyTransportAlgorithm=false`, **cache de rejeu + `timestampStrict`** obligatoires, contraintes de DN du signataire plutôt que confiance en tout certificat du truststore, révocation activée. Le XML Signature Wrapping se contre en validant **quels** éléments sont signés, pas en constatant qu'une signature est valide.
**5. Ne pas réémettre le XML de l'attaquant** (XSLT piloté par l'appelant, logs rendus ailleurs).

---

## Vérification avant de livrer

- [ ] `spring-boot-starter-webservices` (sans tiret) ; aucun `WsConfigurerAdapter` ni `XwsSecurityInterceptor` dans le code.
- [ ] Sources JAXB régénérées en namespace `jakarta.*` ; `jaxb-runtime` et `saaj-impl` présents.
- [ ] `Jaxb2Marshaller` en singleton, `setContextPath`, XXE non désactivé.
- [ ] `PayloadValidatingInterceptor` actif sur les requêtes ; XSD borné (`maxLength`, `maxOccurs`).
- [ ] Client : timeouts via `HttpClientSettings`, pool HttpClient 5 dimensionné si threads virtuels.
- [ ] WS-Security : cache de rejeu + `timestampStrict=true` + SHA-256 ; sinon mTLS via `SslBundle`.
- [ ] `MessageTracing` jamais en `TRACE` en production.
- [ ] Tags d'observation par opération ajoutés (sinon tout est agrégé sous `/services`).
- [ ] Aucun client mobile ou navigateur ne parle SOAP directement.
- [ ] Pas d'objectif d'image native sur ce service (non supporté par Spring WS).
