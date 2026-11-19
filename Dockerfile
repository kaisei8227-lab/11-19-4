# Render 用。GitHub のリポジトリから Render がこのファイルを読んでビルドする

# 1) ビルド
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# 2) 実行
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# 無料プランはメモリが少ないので上限を決めておく
ENV JAVA_OPTS="-Xmx300m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
