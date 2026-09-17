# syntax=docker/dockerfile:1

# Etapa 1: compila el jar con Maven y Java 21. La caché de Maven se reutiliza entre compilaciones,
# así las dependencias no se vuelven a descargar cada vez.
FROM maven:3.9-eclipse-temurin-21 AS compilacion
WORKDIR /fuente
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# Etapa 2: solo lo necesario para correr el backend.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=compilacion /fuente/target/*.jar app.jar
# Mismo UID que el usuario ubuntu del servidor: así puede leer el JSON de Firebase que se monta desde su carpeta.
USER 1000:1000
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
