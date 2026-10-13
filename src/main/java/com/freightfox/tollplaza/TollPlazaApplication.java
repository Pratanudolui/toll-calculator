package com.freightfox.tollplaza;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TollPlazaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TollPlazaApplication.class, args);
    }
}
