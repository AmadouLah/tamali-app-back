# Dockerfile pour Tamali Backend
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache tzdata && \
    addgroup -g 1001 -S spring && \
    adduser -u 1001 -S spring -G spring

COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

RUN mkdir -p /app/uploads /app/logs && \
    chown -R spring:spring /app

USER spring

EXPOSE 10000

ENV JAVA_OPTS="-Xms96m -Xmx380m \
    -XX:+UseSerialGC \
    -XX:MaxMetaspaceSize=120m \
    -XX:CompressedClassSpaceSize=24m \
    -XX:ReservedCodeCacheSize=24m \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=1 \
    -XX:MinHeapFreeRatio=20 \
    -XX:MaxHeapFreeRatio=40 \
    -XX:+UseStringDeduplication \
    -Djava.awt.headless=true \
    -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.address=0.0.0.0 -jar app.jar"]

