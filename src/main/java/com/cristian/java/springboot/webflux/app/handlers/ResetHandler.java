package com.cristian.java.springboot.webflux.app.handlers;

import com.cristian.java.springboot.webflux.app.models.Category;
import com.cristian.java.springboot.webflux.app.models.Product;
import com.cristian.java.springboot.webflux.app.services.ProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import org.springframework.http.codec.ServerSentEvent;
import java.time.Duration;

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

    // --- 1. LÓGICA CENTRAL DE RESETEO (Reutilizable) ---
    private Mono<Void> performDatabaseReset() {
        Category electronics = new Category("Electronico");
        Category sport = new Category("Deporte");
        Category computing = new Category("Computacion");
        Category furniture = new Category("Muebles");

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
                // Al finalizar la limpieza, emitimos la señal al canal
                .doOnComplete(() -> sink.tryEmitNext("RELOAD_DATA"))
                .then(); // Convertimos el resultado final a Mono<Void>
    }

    // --- 2. CRONJOB INTERNO (Se ejecuta a los 0, 15, 30 y 45 min) ---
    @Scheduled(cron = "0 */15 * * * *")
    public void autoCleanDatabase() {
        System.out.println("⏰ [CRONJOB] Ejecutando limpieza de BD programada...");

        // ¡Crucial! Al no haber una petición HTTP, debemos suscribirnos manualmente
        performDatabaseReset().subscribe(
                null, // onNext (no devuelve nada porque es Void)
                error -> System.err.println("❌ Error en el cronjob: " + error.getMessage()),
                () -> System.out.println("✅ Limpieza automática completada.")
        );
    }

    // --- 3. ENDPOINT MANUAL (Activado por GitHub Actions o manual) ---
    public Mono<ServerResponse> resetDatabase(ServerRequest request) {
        String secret = request.headers().firstHeader("X-Reset-Secret");

        if (!resetSecret.equals(secret)) {
            return ServerResponse.status(403).bodyValue("Acceso denegado");
        }

        // Llamamos al método central y devolvemos la respuesta HTTP
        return performDatabaseReset()
                .then(ServerResponse.ok().bodyValue("Base de datos reiniciada con éxito"));
    }

    // --- 4. ENDPOINT PÚBLICO SSE (Para Angular) ---
    public Mono<ServerResponse> streamEvents(ServerRequest request) {
        Flux<ServerSentEvent<String>> events = sink.asFlux()
                .map(data -> ServerSentEvent.<String>builder().data(data).build());

        Flux<ServerSentEvent<String>> keepAlive = Flux.interval(Duration.ofSeconds(15))
                .map(tick -> ServerSentEvent.<String>builder().comment("keep-alive").build());

        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(Flux.merge(events, keepAlive), ServerSentEvent.class);
    }

    // --- 5. ENDPOINT PING (El verdadero despertador para GitHub Actions) ---
    public Mono<ServerResponse> pingServer(ServerRequest request) {
        System.out.println("🔔 [PING] Servidor despertado exitosamente por agente externo.");
        return ServerResponse.ok().bodyValue("Pong - Servidor activo y despierto");
    }
}