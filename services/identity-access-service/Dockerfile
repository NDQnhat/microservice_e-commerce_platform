FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build/libs/identity-access-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
