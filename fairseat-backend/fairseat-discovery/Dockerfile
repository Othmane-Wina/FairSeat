FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/fairseat-discovery-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
