FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY build/libs/order-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
