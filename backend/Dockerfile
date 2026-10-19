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

# Non-root user for security and su-exec for entrypoint privilege dropping
RUN apk add --no-cache su-exec \
    && addgroup -S edufit && adduser -S edufit -G edufit \
    && mkdir -p /app/var/credentials \
    && chown -R edufit:edufit /app

COPY --from=builder --chown=edufit:edufit /workspace/app/target/app-1.0-SNAPSHOT.jar app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/app/docker-entrypoint.sh"]

