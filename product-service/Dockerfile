# Use an official lightweight Java runtime image
FROM eclipse-temurin:25-jre-jammy
# Set the working directory inside the container
WORKDIR /app
# Copy the compiled JAR file from your build directory into the container
COPY build/libs/*.jar app.jar
# Expose the port your Spring Boot app runs on
EXPOSE 8080
# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]