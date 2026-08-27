package com.cristian.java.springboot.webflux.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class SpringbootWebfluxRestApplication{

    public static void main(String[] args) {
        SpringApplication.run(SpringbootWebfluxRestApplication.class, args);
    }

}