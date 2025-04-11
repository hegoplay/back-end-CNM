# Use a lightweight OpenJDK base image
FROM bellsoft/liberica-openjdk-alpine:21.0.5-11

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from the target directory
COPY target/*.jar app.jar

# Expose the port your Spring Boot app runs on (default is 8080)
EXPOSE 3001

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]