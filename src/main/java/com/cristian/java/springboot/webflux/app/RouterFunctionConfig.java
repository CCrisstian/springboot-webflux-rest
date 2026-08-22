package com.cristian.java.springboot.webflux.app;

import com.cristian.java.springboot.webflux.app.handlers.ProductHandler;
import com.cristian.java.springboot.webflux.app.handlers.ResetHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

// Importaciones estáticas para mantener el código limpio y legible (ej. usar GET() en lugar de RequestPredicates.GET())
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * Clase de configuración que reemplaza a los clásicos @RestController.
 * Define la tabla de rutas (URL mapping) de la aplicación de forma funcional y declarativa.
 */
@Configuration
public class RouterFunctionConfig {

    /**
     * Define un Bean manejado por Spring que contiene las rutas de nuestra API.
     *
     * @param handler El ProductHandler que inyecta Spring automáticamente y contiene la lógica de negocio.
     * @return RouterFunction que mapea las solicitudes HTTP a los métodos del Handler.
     */
    @Bean
    public RouterFunction<ServerResponse> routes(ProductHandler handler, ResetHandler resetHandler) {
        // Enrutamiento funcional: Vincula un predicado HTTP (Verbo + URL) a una función específica del Handler.
        return route(GET("/products"), handler::list)
                .andRoute(GET("/products/{id}"), handler::details)
                .andRoute(POST("/products"), handler::create)
                .andRoute(PUT("/products/{id}"), handler::update)
                .andRoute(DELETE("/products/{id}"), handler::remove)
                .andRoute(POST("/api/internal/reset"), resetHandler::resetDatabase)
                .andRoute(GET("/api/events"), resetHandler::streamEvents);
    }
}