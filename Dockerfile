# Stage 1: Build the application using Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy everything and build the project
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the application using a lightweight JDK image
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Copy the jar from the previous build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (optional, Render detects it via settings)
EXPOSE 8080

# Start the app
ENTRYPOINT ["java", "-jar", "app.jar"]
