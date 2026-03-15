# --- Build stage ---
FROM clojure:temurin-21-lein AS builder

WORKDIR /app

# Cache dependencies first
COPY project.clj .
RUN lein deps

COPY . .
RUN lein uberjar

# --- Run stage ---
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /app/target/uberjar/revenu-api-*-standalone.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
