# Build stage: compiles the app with Maven. The tests run before you deploy,
# not here, so the image builds quickly.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Dependencies first, so this layer is reused until pom.xml changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Run stage: just the Java runtime and the app.
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build /app/target/*.jar app.jar
USER spring

# Small cloud instances have little memory, so let the heap use most of it.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# The app listens on $PORT (Render sets it), or 8080 when PORT isn't set.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
