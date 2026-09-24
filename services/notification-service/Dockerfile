FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build/libs/notification-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]
