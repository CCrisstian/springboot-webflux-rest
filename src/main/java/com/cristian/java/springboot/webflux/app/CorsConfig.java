package com.cristian.java.springboot.webflux.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class CorsConfig implements WebFluxConfigurer {

    // Lee la variable de entorno FRONTEND_URL.
    // Si no existe usa "http://localhost:4200" por defecto.
    @Value("${FRONTEND_URL:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Habilita CORS para TODAS las rutas (/products, etc.)
                .allowedOrigins(frontendUrl) // Permite solo a la URL de Angular
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Verbos HTTP permitidos
                .allowedHeaders("*") // Permite cualquier cabecera
                .allowCredentials(true); // Necesario si en el futuro envías cookies o tokens
    }
}