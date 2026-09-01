# syntax=docker/dockerfile:1

# --- Construction ----------------------------------------------------------
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /src

# Le wrapper et le POM d'abord : cette couche ne change qu'au changement de dépendance,
# et le cache Maven monté ci-dessous évite de retélécharger l'internet à chaque build.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
# En Boot 4.1, -DskipTests NE SAUTE PLUS le traitement AOT : il compile les tests pour rien.
# -Dmaven.test.skip=true les ignore vraiment. Les tests, eux, tournent en CI.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -Dmaven.test.skip=true package

# Découpe le jar en couches : dépendances, chargeur, application. Une correction d'une
# ligne ne réexpédie alors que quelques kilo-octets, pas soixante mégaoctets.
#
# En Boot 4.1, `extract --layers` produit un jar ALLÉGÉ (118 Ko) accompagné d'un dossier
# lib/ — et non l'arborescence BOOT-INF/ des versions précédentes. Il n'y a donc pas de
# `app.jar` à la racine : on renomme, sinon l'image démarre sur
# « Error: Unable to access jarfile app.jar ».
RUN java -Djarmode=tools -jar target/backend-*.jar extract --layers --destination extracted \
 && mv extracted/application/backend-*.jar extracted/application/app.jar

# --- Exécution -------------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime
RUN addgroup -S sd && adduser -S sd -G sd
WORKDIR /app

COPY --from=build /src/extracted/dependencies/ ./
COPY --from=build /src/extracted/spring-boot-loader/ ./
COPY --from=build /src/extracted/snapshot-dependencies/ ./
COPY --from=build /src/extracted/application/ ./

USER sd
EXPOSE 8080

# MaxRAMPercentage plutôt qu'un -Xmx en dur : la JVM lit la limite du conteneur, ce qui
# évite de la reconfigurer à chaque changement de dimensionnement.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
