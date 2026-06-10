FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/fairseat-marketplace-*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
