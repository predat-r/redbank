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

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
