FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
