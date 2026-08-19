# Stage 1: Build stage
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and POM to cache dependencies layer
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code and package application
COPY src ./src
RUN ./mvnw package -DskipTests && cp target/*.jar app.jar

# Stage 2: Runtime stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy built JAR from builder stage
COPY --from=builder /app/app.jar app.jar

EXPOSE 8080

USER appuser

ENV JAVA_OPTS="-Xms64m -Xmx224m -XX:MaxMetaspaceSize=100m -XX:ReservedCodeCacheSize=40m -Xss256k -XX:+UseG1GC -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
