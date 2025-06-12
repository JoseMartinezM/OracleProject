# ===== Stage 1: Build the application =====
FROM maven:3.9.6-eclipse-temurin-22 AS build

WORKDIR /app

# Copy the pom and source code
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean install -DskipTests

# ===== Stage 2: Run the application =====
FROM openjdk:22

WORKDIR /app

# Copy only the built JAR from the previous stage
COPY --from=build /app/target/MyTodoList-0.0.1-SNAPSHOT.jar MyTodoList.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "MyTodoList.jar"]
