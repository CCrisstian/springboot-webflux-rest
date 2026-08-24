# Usamos una imagen oficial de Java 21
FROM eclipse-temurin:21-jdk-alpine

# Creamos un directorio dentro del contenedor
WORKDIR /app

# Copiamos el archivo JAR compilado desde la carpeta target al contenedor
COPY target/springboot-webflux-rest-0.0.1-SNAPSHOT.jar app.jar

# Exponemos el puerto 8080 (el que usará Render internamente)
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]