# syntax=docker/dockerfile:1

# --- Étape de construction -------------------------------------------------
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /src

# Le wrapper d'abord : cette couche ne change presque jamais.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
# En Boot 4.1, -DskipTests ne saute plus le traitement AOT : utiliser -Dmaven.test.skip.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -Dmaven.test.skip=true package

# --- Image d'exécution -----------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime
RUN addgroup -S crm && adduser -S crm -G crm
WORKDIR /app
COPY --from=build /src/target/legacy-crm-*.jar app.jar
USER crm
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
