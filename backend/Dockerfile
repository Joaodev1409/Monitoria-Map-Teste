# Etapa 1: compila o projeto com Maven (as dependências ficam em cache entre builds).
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Etapa 2: imagem final só com o JRE e o .jar (bem menor, sem código-fonte nem Maven).
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /app/target/monitoria-map-*.jar app.jar
USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
