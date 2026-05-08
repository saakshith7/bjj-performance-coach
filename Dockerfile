# Stage 1 - Build the application
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Copy maven wrapper and pom first (layer caching)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

#Download dependencies
RUN ./mvnw dependency:go-offline -B

#Copy source code
COPY src src

#Build the JAR skipping tests
RUN ./mvnw clean package -DskipTests

#Stage 2 - Run the application
FROM eclipse-temurin:17-jre AS final

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

#Expose port
EXPOSE 8080

#Run the application
ENTRYPOINT ["java","-jar","app.jar"]

