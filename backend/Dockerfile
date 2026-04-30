# Build stage
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /build

# Install Maven 3.9.6
RUN apt-get update && apt-get install -y curl && \
    curl -fsSL https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.tar.gz \
    | tar -xz -C /opt && \
    ln -s /opt/apache-maven-3.9.6/bin/mvn /usr/local/bin/mvn && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Build
COPY src ./src
RUN mvn package -DskipTests -q

# Runtime stage
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# Non-root user
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

COPY --from=build /build/target/dcms-*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

ENV PORT=8081
EXPOSE 8081

# SIGTERM handling: Spring Boot shuts down gracefully with server.shutdown=graceful
ENTRYPOINT ["java", "-jar", "app.jar"]
