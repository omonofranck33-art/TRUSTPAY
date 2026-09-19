# ===== Étape 1 : construire le programme (compilation avec Maven) =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline        # télécharge les bibliothèques (mis en cache)
COPY src ./src
RUN mvn -q package -DskipTests          # crée le fichier target/*.jar

# ===== Étape 2 : image finale légère, juste de quoi exécuter Java =====
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Limite la mémoire de Java pour rester sous les 512 Mo du plan gratuit
ENV JAVA_TOOL_OPTIONS="-Xmx320m -XX:+UseSerialGC"
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
