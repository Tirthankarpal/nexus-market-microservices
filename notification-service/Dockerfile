FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY build/libs/notification-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
