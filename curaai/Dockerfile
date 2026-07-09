# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy only the pom first so dependency downloads are cached
# between builds when only source files change.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests


# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Render injects the port to listen on via the PORT env var.
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]
