# ---- Build Stage ----
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY common/pom.xml common/
COPY proto/ proto/
COPY apigateway/pom.xml apigateway/
COPY auth/pom.xml auth/
COPY email/pom.xml email/
COPY merchant/pom.xml merchant/
COPY role/pom.xml role/
COPY transaction/pom.xml transaction/
COPY cashier/pom.xml cashier/
COPY category/pom.xml category/
COPY product/pom.xml product/
COPY order/pom.xml order/
COPY order_item/pom.xml order_item/
COPY common/src common/src
COPY email/src email/src

RUN mvn clean package -pl email -am -DskipTests -q

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/email/target/email-*-fat.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
