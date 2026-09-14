FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /build
COPY . .
RUN ./gradlew :bento-bot:bootJar --no-daemon -q

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/bento-bot/build/libs/bento-bot-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
