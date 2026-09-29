# Stage 1: Build the multi-module application
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy Maven wrapper & configurations
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Copy all module POMs and source directories
COPY platform/ platform/
COPY modules/ modules/
COPY app/ app/

RUN chmod +x ./mvnw && ./mvnw -B package -DskipTests

# Stage 2: Minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S edufit && adduser -S edufit -G edufit

COPY --from=builder --chown=edufit:edufit /workspace/app/target/app-1.0-SNAPSHOT.jar app.jar

USER edufit

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
