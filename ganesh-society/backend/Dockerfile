
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Xms128m", "-Xmx512m", "-jar", "app.jar"]
