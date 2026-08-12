# Stage 1: Build stage
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper, pom.xml and source code
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
COPY src ./src

# Package application
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy built JAR from builder stage
COPY --from=builder /app/target/redbank-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
