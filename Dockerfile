FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

RUN chmod +x gradlew
RUN ./gradlew --no-daemon bootJar
RUN JAR_FILE=$(find build/libs -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' | head -n 1) \
    && cp "$JAR_FILE" app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system spring \
    && useradd --system --gid spring --create-home spring

COPY --from=builder /workspace/app.jar app.jar

ENV TZ=Asia/Seoul
ENV SERVER_PORT=8080
ENV JAVA_OPTS=""

EXPOSE 8080

USER spring

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
