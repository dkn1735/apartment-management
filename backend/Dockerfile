# Build bằng JDK 25, chạy trên JRE 25, user không phải root
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sh mvnw -B -q dependency:go-offline
COPY src/ src/
RUN sh mvnw -B -q package -DskipTests && cp target/*.jar app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/app.jar app.jar
USER app
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-Duser.timezone=UTC", "-jar", "/app/app.jar"]
