FROM openjdk:22

# Set working directory
WORKDIR /app

# Copy the compiled Spring Boot JAR
COPY target/MyTodoList-0.0.1-SNAPSHOT.jar MyTodoList.jar

# Copy the wallet directory into the container (adjust path as needed)
COPY Wallet_LocalDB /app/wallet

# Expose the app port
EXPOSE 8081

# Run the app
ENTRYPOINT ["java", "-jar", "MyTodoList.jar"]
