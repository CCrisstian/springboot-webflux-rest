package com.cristian.java.springboot.webflux.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    // Lee la variable de entorno FRONTEND_URL.
    @Value("${FRONTEND_URL:http://localhost:4200}")
    private String frontendUrl;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Permite solo a la URL de Angular
        corsConfig.setAllowedOrigins(Arrays.asList(frontendUrl));
        // Verbos HTTP permitidos
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Permite cualquier cabecera
        corsConfig.setAllowedHeaders(Arrays.asList("*"));
        // Necesario para Server-Sent Events y credenciales
        corsConfig.setAllowCredentials(true);
        // Tiempo en caché para la configuración de CORS
        corsConfig.setMaxAge(8000L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplica a TODAS las rutas
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}