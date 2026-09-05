# ---- Build Stage ----
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY common/pom.xml common/
COPY apigateway/pom.xml apigateway/
COPY auth/pom.xml auth/
COPY cashier/pom.xml cashier/
COPY category/pom.xml category/
COPY email/pom.xml email/
COPY merchant/pom.xml merchant/
COPY order/pom.xml order/
COPY order_item/pom.xml order_item/
COPY product/pom.xml product/
COPY role/pom.xml role/
COPY transaction/pom.xml transaction/
COPY user/pom.xml user/
COPY db-migration/pom.xml db-migration/

COPY common/src common/src
COPY apigateway/src apigateway/src

RUN --mount=type=cache,target=/root/.m2 mvn clean package -pl apigateway -am -DskipTests -q -Dmaven.wagon.http.retryHandler.count=5

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/apigateway/target/apigateway-*-fat.jar app.jar
COPY chaos.yaml .
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"]
