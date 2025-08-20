## Multi-stage build for optimized production image
##FROM openjdk:17-jdk-slim as builder
#FROM public.ecr.aws/docker/library/eclipse-temurin:17-jdk-jammy AS builder
#
## Set working directory
#WORKDIR /app
#
## Copy Maven wrapper and pom.xml
#COPY mvnw .
#COPY .mvn .mvn
#COPY pom.xml .
#
## Download dependencies (cached layer)
##RUN ./mvnw dependency:go-offline -B
#RUN chmod +x mvnw && ./mvnw -v && ./mvnw dependency:go-offline -B
#
## Copy source code
#COPY src src
#
## Build application
#RUN ./mvnw clean package -DskipTests
#
## Production stage
#FROM public.ecr.aws/docker/library/eclipse-temurin:17-jre-jammy
#
## Create non-root user for security
#RUN groupadd -r banking && useradd -r -g banking banking
#
## Set working directory
#WORKDIR /app
#
## Copy JAR from builder stage
#COPY --from=builder /app/target/banking-system-*.jar app.jar
#
## Change ownership to non-root user
#RUN chown -R banking:banking /app
#
## Switch to non-root user
#USER banking
#
## Expose port
#EXPOSE 8080
#
## Health check
#HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
#  CMD curl -f http://localhost:8080/actuator/health || exit 1
#
## Run application
#ENTRYPOINT ["java", "-jar", "app.jar"]\


# ---- builder: Maven + JDK ----
FROM public.ecr.aws/docker/library/maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -v && mvn -B -DskipTests=true clean package

# ---- runtime: JRE only ----
FROM public.ecr.aws/docker/library/eclipse-temurin:17-jre-jammy

# Install curl for HEALTHCHECK
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r banking && useradd -r -g banking banking

WORKDIR /app
COPY --from=builder /app/target/banking-system-*.jar /app/app.jar
RUN chown -R banking:banking /app
USER banking

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]
