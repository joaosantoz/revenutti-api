FROM clojure:temurin-21-lein AS builder

WORKDIR /app

COPY project.clj .
RUN lein deps

COPY . .
RUN lein uberjar && cp target/uberjar/revenu-api-*-standalone.jar app.jar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/app.jar app.jar

EXPOSE 3000

CMD ["java", "-jar", "app.jar"]
