FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests dependency:go-offline

COPY config ./config
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp verify

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S edufit && adduser -S edufit -G edufit
COPY --from=build /workspace/target/edufit-backend-*.jar app.jar
USER edufit
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
