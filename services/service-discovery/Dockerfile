FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build/libs/service-discovery-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
