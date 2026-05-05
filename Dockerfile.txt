FROM openjdk:17-jdk-slim

WORKDIR /app

COPY . .

RUN chmod +x mvnw && ./mvnw clean package -DskipTests

CMD ["java", "-jar", "target/formbuilder-0.0.1-SNAPSHOT.jar"]