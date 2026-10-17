# Etapa 1: compila o projeto com Maven (as dependências ficam em cache entre builds).
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests
# O jar "gordo" é extraído (app.jar + lib/): é nesse formato que o cache de classes funciona.
RUN cp target/monitoria-map-*.jar app.jar && java -Djarmode=tools -jar app.jar extract --destination application

# Etapa 2: imagem final só com o JRE e a aplicação (bem menor, sem código-fonte nem Maven).
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 app

# Flags pensadas para a máquina gratuita do Render (0,1 de CPU, 512 MB), onde o que pesa é subir:
# - TieredStopAtLevel=1: só o compilador rápido da JVM; sobe mais rápido (o pico menor não importa aqui);
# - UseSerialGC: o coletor mais simples, sem threads extras disputando a pouca CPU;
# - Xss512k: pilhas menores por thread.
# Precisam ser as mesmas no treino abaixo e na execução, senão a JVM descarta o cache de classes.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xss512k"

COPY --from=build /app/application application

# Treino do cache de classes (CDS): o app sobe uma vez, aqui no build, até o contexto do Spring
# ficar pronto (spring.context.exit=onRefresh) e encerra, gravando as classes já carregadas e
# verificadas em application.jsa. Na subida de verdade a JVM só mapeia esse arquivo.
# Não há banco no build, então o treino desliga tudo que conectaria nele; o segredo é fictício
# e só existe neste comando (em produção vale o que o Render gerou).
RUN APP_SESSAO_SEGREDO=treino-do-cds-sem-uso-em-producao-0123456789 \
    java -XX:ArchiveClassesAtExit=application/application.jsa -Dspring.context.exit=onRefresh $JAVA_OPTS \
      -Dspring.flyway.enabled=false \
      -Dspring.jpa.hibernate.ddl-auto=none \
      -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
      -Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
      -jar application/app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java -XX:SharedArchiveFile=application/application.jsa $JAVA_OPTS -jar application/app.jar"]
