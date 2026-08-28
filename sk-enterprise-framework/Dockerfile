# syntax=docker/dockerfile:1
# =====================================================================
# Multi-stage build for basic-crud-sample (JDK 25)
# build context = sk-enterprise-framework/
# =====================================================================

FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /workspace
# 의존성 캐시를 위해 pom 먼저 복사
COPY pom.xml ./
COPY framework-core ./framework-core
COPY framework-samples ./framework-samples
COPY framework-starters ./framework-starters
RUN mvn -B -pl framework-samples/basic-crud-sample -am clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/framework-samples/basic-crud-sample/target/basic-crud-sample-*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="" \
    SPRING_PROFILES_ACTIVE="prod"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
