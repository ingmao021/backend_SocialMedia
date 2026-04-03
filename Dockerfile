#   STAGE 1: Build del JAR
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copiar archivos de Maven
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

#Asegurar permisos de ejecución
RUN chmod +x mvnw

# Descargar dependencias antes de copiar el código (optimiza cache)
RUN ./mvnw dependency:go-offline

# Copiar el código fuente
COPY src src

# Compilar el proyecto
RUN ./mvnw clean package -DskipTests


#   STAGE 2: Imagen final

FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiar el JAR desde el stage anterior
COPY --from=builder /app/target/*.jar app.jar

# Exponer el puerto (Render lo ignora, pero es buena práctica)
EXPOSE 8080

# Variables de entorno recomendadas
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV PORT=8080

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080} --spring.profiles.active=prod"]
