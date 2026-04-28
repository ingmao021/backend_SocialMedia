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


#   STAGE 2: Imagen final optimizada

FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiar el JAR desde el stage anterior
COPY --from=builder /app/target/*.jar app.jar

# Exponer el puerto (Render lo ignora, pero es buena práctica)
EXPOSE 8080

# Variables de entorno optimizadas para Render
ENV PORT=8080
ENV WEB_CONCURRENCY=1  # Render maneja el scaling automáticamente

# JVM flags optimizados para startup rápido y memoria eficiente
ENV JAVA_OPTS="\
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.profiles.active=prod \
  -Dserver.port=${PORT}"

# Health check para Render
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${PORT}/api/auth/status || exit 1

# Comando de inicio optimizado
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
