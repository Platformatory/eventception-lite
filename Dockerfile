FROM maven:3.8.7-openjdk-18 AS build

WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN mvn clean package -DskipTests

FROM confluentinc/cp-kafka-connect:7.5.0

USER root

RUN wget https://github.com/Aiven-Open/http-connector-for-apache-kafka/releases/download/v0.7.0/http-connector-for-apache-kafka-0.7.0.tar && \
    tar -xvf http-connector-for-apache-kafka-0.7.0.tar -C /usr/share/java/kafka && \
    confluent-hub install --no-prompt confluentinc/kafka-connect-jdbc:10.7.12 && \
    confluent-hub install --no-prompt confluentinc/kafka-connect-s3:10.5.15

USER appuser

WORKDIR /app

COPY --from=build /app/target/eventception-apiprocessor-1.0-SNAPSHOT-jar-with-dependencies.jar /app/eventception-apiprocessor-1.0-SNAPSHOT-jar-with-dependencies.jar

CMD ["java", "-jar", "/app/eventception-apiprocessor-1.0-SNAPSHOT-jar-with-dependencies.jar"]
