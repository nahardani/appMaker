FROM eclipse-temurin:${java.version}-jre
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
