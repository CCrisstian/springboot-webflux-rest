package com.cristian.java.springboot.webflux.app.handlers;

import com.cristian.java.springboot.webflux.app.models.Category;
import com.cristian.java.springboot.webflux.app.models.Product;
import com.cristian.java.springboot.webflux.app.services.ProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;

@Component
public class ResetHandler {

    private final ProductService service;
    private final ReactiveMongoTemplate mongoTemplate;

    // Canal reactivo para emitir los eventos SSE al frontend
    private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

    // Inyectamos el valor de forma segura
    @Value("${api.reset.secret}")
    private String resetSecret;

    public ResetHandler(ProductService service, ReactiveMongoTemplate mongoTemplate) {
        this.service = service;
        this.mongoTemplate = mongoTemplate;
    }

    public Mono<ServerResponse> resetDatabase(ServerRequest request) {

        // 1. Validamos la contraseña en los Headers
        String secret = request.headers().firstHeader("X-Reset-Secret");

        // Comparamos el secreto inyectado con el que envió el usuario
        if (!resetSecret.equals(secret)) {
            return ServerResponse.status(403).bodyValue("Acceso denegado");
        }

        // 2. Preparamos los datos
        Category electronics = new Category("Electronico");
        Category sport = new Category("Deporte");
        Category computing = new Category("Computacion");
        Category furniture = new Category("Muebles");

        // 3. Ejecutamos el flujo reactivo de borrado e inserción
        return this.mongoTemplate.dropCollection("products")
                .then(this.mongoTemplate.dropCollection("categories"))
                .thenMany(Flux.just(electronics, sport, computing, furniture)
                        .flatMap(service::saveCategory))
                .thenMany(Flux.just(
                                new Product("TV Panasonic", 342.67, electronics),
                                new Product("Sony Camara", 500.99, electronics),
                                new Product("Apple iPod", 245.89, electronics),
                                new Product("Notebook Sony", 2000.67, computing),
                                new Product("Hewlett Packard Multifuncional Impresora", 600.55, computing),
                                new Product("Bianchi Bicicleta", 3500.89, sport),
                                new Product("Mueble mica 5 cajones", 250.78, furniture)
                        ).flatMap(product -> {
                            product.setCreatedAt(LocalDateTime.now());
                            return service.save(product);
                        })
                )
                .then(ServerResponse.ok().bodyValue("Base de datos reiniciada con éxito"))
                // Al finalizar la limpieza, emitimos la señal para el frontend
                .doOnSuccess(response -> sink.tryEmitNext("RELOAD_DATA"));
    }

    // Endpoint público para que Angular escuche los eventos (SSE)
    public Mono<ServerResponse> streamEvents(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(sink.asFlux(), String.class);
    }
}