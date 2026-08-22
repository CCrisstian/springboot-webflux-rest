package com.cristian.java.springboot.webflux.app.handlers;

import com.cristian.java.springboot.webflux.app.models.Product;
import com.cristian.java.springboot.webflux.app.services.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDateTime;

/**
 * Handler Funcional: Sustituye al @RestController tradicional en el modelo reactivo.
 * Encapsula la lógica para procesar los ServerRequest y construir los ServerResponse.
 */
@Component
public class ProductHandler {

    private final ProductService productService;
    private final Validator validator;

    // Inyección de dependencias recomendada vía constructor
    public ProductHandler(ProductService productService, Validator validator) {
        this.productService = productService;
        this.validator = validator;
    }

    /**
     * Lista todos los productos.
     * Se delega el Publisher (Flux) directamente al body; WebFlux gestiona la suscripción automáticamente.
     */
    public Mono<ServerResponse> list(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(productService.findAlL(), Product.class);
    }

    /**
     * Obtiene los detalles de un producto mediante su ID.
     */
    public Mono<ServerResponse> details(ServerRequest request) {
        String id = request.pathVariable("id");

        return productService.findById(id)
                // Si el producto se emite, construimos una respuesta 200 OK
                .flatMap(product -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(product))
                // Flujo alternativo: si el Mono está vacío, retornamos 404 Not Found
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * Valida, crea y persiste un nuevo producto.
     */
    public Mono<ServerResponse> create(ServerRequest request) {
        Mono<Product> productMono = request.bodyToMono(Product.class);

        return productMono
                .flatMap(product -> {
                    // 1. Validación manual del objeto entrante (requerido en handlers funcionales)
                    Errors errors = new BeanPropertyBindingResult(product, Product.class.getName());
                    validator.validate(product, errors);

                    if (errors.hasErrors()) {
                        return Flux.fromIterable(errors.getFieldErrors())
                                .map(fieldError -> "El campo " + fieldError.getField() + " " + fieldError.getDefaultMessage())
                                .collectList()
                                .flatMap(list -> ServerResponse.badRequest().bodyValue(list));
                    }

                    // 2. Preparación y persistencia
                    product.setCreatedAt(LocalDateTime.now());

                    return productService.save(product)
                            // 3. Construcción de la respuesta 201 Created con la URI del nuevo recurso
                            .flatMap(productDB -> ServerResponse
                                    .created(URI.create("/products/" + productDB.getId()))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(productDB));
                });
    }

    /**
     * Actualiza un producto fusionando el estado actual de la BD con el payload del request.
     */
    public Mono<ServerResponse> update(ServerRequest request) {
        Mono<Product> productMono = request.bodyToMono(Product.class);
        String id = request.pathVariable("id");

        Mono<Product> productDB = productService.findById(id);

        // zipWith combina ambos Monos y emite una tupla solo si AMBOS publican un elemento.
        return productDB.zipWith(productMono, (db, req) -> {
                    // Mutamos la entidad recuperada con los datos entrantes
                    db.setName(req.getName());
                    db.setPrice(req.getPrice());
                    db.setCategory(req.getCategory());
                    return db;
                })
                .flatMap(product -> ServerResponse
                        .created(URI.create("/products/" + product.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(productService.save(product), Product.class))
                // Si zipWith no emite nada (porque productDB estaba vacío), caemos aquí
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * Elimina un producto por ID devolviendo 204 No Content en caso de éxito.
     */
    public Mono<ServerResponse> remove(ServerRequest request) {
        String id = request.pathVariable("id");
        Mono<Product> productMono = productService.findById(id);

        return productMono
                .flatMap(productDB -> ServerResponse
                        .status(HttpStatus.NO_CONTENT)
                        .body(productService.delete(productDB), Void.class))
                .switchIfEmpty(ServerResponse.notFound().build());
    }
}