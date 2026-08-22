package com.cristian.java.springboot.webflux.app.repositories;

import com.cristian.java.springboot.webflux.app.models.Category;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface CategoryRepository extends ReactiveMongoRepository<Category, String> {
}
