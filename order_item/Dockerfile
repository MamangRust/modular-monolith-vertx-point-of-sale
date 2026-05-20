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
COPY order_item/src order_item/src

RUN mvn clean package -pl order_item -am -DskipTests -q

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/order_item/target/order_item-*-fat.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
