---
name: spring-boot-4
description: À charger pour toute écriture ou modification de code Spring Boot sur ce projet. Contient les écarts entre Spring Boot 3 et Spring Boot 4.1 que les modèles confondent le plus souvent — noms de starters, packages, dépendances de test.
---

# Spring Boot 4.1 — écarts avec Spring Boot 3

Ce projet utilise **Spring Boot 4.1.1** et **Spring Framework 7**. Spring Boot 3.5 est en fin de
vie depuis le 30 juin 2026. Une grande partie de ce que tu sais de Boot 3 ne s'applique plus.

Ce fichier ne recopie pas la documentation : il liste uniquement ce qui a changé et que tu
risques d'écrire de travers.

## 1. Les starters ont été renommés

La modularisation de Boot 4 a découpé les starters. Les anciens noms existent encore mais sont
dépréciés et seront supprimés. **Utilise systématiquement les nouveaux.**

| N'écris pas | Écris |
|---|---|
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| `spring-boot-starter-web-services` | `spring-boot-starter-webservices` |
| *(rien — Flyway était autoconfiguré)* | `spring-boot-starter-flyway` |

Des bibliothèques qui ne demandaient aucun starter en Boot 3, comme Flyway ou Liquibase, ont
maintenant le leur : leurs autoconfigurations y ont été déplacées. Sans le starter, elles ne
s'activent pas.

## 2. Un starter de test par technologie

`spring-boot-starter-test` seul ne suffit plus. Chaque technologie a son starter de test, nommé
`spring-boot-starter-<techno>-test`, qui amène `spring-boot-starter-test` de façon transitive.

| Techno | Starter de test |
|---|---|
| Web MVC | `spring-boot-starter-webmvc-test` |
| Data JPA | `spring-boot-starter-data-jpa-test` |
| GraphQL | `spring-boot-starter-graphql-test` |
| Web Services | `spring-boot-starter-webservices-test` |
| Security | `spring-boot-starter-security-test` |

**Symptôme quand il manque :** `@AutoConfigureMockMvc`, `@DataJpaTest` et consorts échouent avec
des erreurs de contexte peu lisibles — des beans attendus sont absents. Le réflexe n'est pas de
bricoler la configuration de test, c'est d'ajouter le starter manquant.

## 3. Les packages ont suivi

Chaque module vit désormais sous `org.springframework.boot.<module>`. Les imports de Boot 3
peuvent ne plus résoudre. Vérifie plutôt que de supposer.

## 4. Autres changements structurants

- **Jackson 3** est la valeur par défaut. Les noms de packages Jackson ont changé — vérifie les
  imports au lieu de reprendre ceux de Boot 3.
- **Spring Security 7** : la configuration a évolué depuis la 6.x.
- **Hibernate 7**, **Jakarta EE 11**, **Tomcat 11**.
- **Tout ce qui était déprécié en 3.x a été supprimé** : anciens endpoints Actuator, anciennes
  propriétés de configuration. Si tu proposes une propriété, assure-toi qu'elle existe encore.
- **JSpecify** remplace les annotations de nullité précédentes.
- **Java** : 17 minimum, support de première classe pour Java 25, compatible jusqu'à Java 26.
  Ce projet est en **Java 25**.

## 5. Spécificités du projet

- Les services SOAP sont exposés sur **`/services`** par défaut en Boot 4
  (`spring.webservices.path` pour changer). Ce n'est plus `/ws`.
- Le module `spring-boot-docker-compose` est présent en portée `optional`. Il démarre les
  conteneurs et fournit les `JdbcConnectionDetails`. **Ne déclare jamais de
  `spring.datasource.url`, `username` ou `password`** — ils viendraient contredire la
  configuration détectée.

## 6. Si tu es bloqué

Une échappatoire existe pour faire démarrer un projet en cours de migration :
`spring-boot-starter-classic` et `spring-boot-autoconfigure-classic` rétablissent le
comportement groupé de Boot 3. **Ne les utilise pas sur ce projet** — ils masquent précisément
ce que la formation cherche à faire comprendre. Signale-moi le blocage à la place.

## 7. Nouveautés de la 4.1

Utiles à connaître, pas nécessaires ici : support gRPC, atténuation SSRF via `InetAddressFilter`
sur les clients HTTP, propagation automatique du contexte d'observabilité vers `@Async`,
versionnage d'API via `@HttpExchange`.
