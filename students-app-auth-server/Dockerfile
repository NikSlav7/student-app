FROM eclipse-temurin:17-jdk AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /students-auth
COPY --from=build /build/target/studentes-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 31212
CMD ["java", "-jar", "app.jar"]
