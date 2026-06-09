ARG DOCKER_REGISTRY=docker.m.daocloud.io/library/

FROM ${DOCKER_REGISTRY}maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY settings.xml /root/.m2/settings.xml

COPY pom.xml .

RUN mvn dependency:go-offline -B -Dmaven.test.skip=true

COPY src ./src

RUN mvn clean package -B -Dmaven.test.skip=true && \
    rm -rf /root/.m2/repository

FROM ${DOCKER_REGISTRY}eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 9033

ENTRYPOINT ["java", "-jar", "app.jar"]
