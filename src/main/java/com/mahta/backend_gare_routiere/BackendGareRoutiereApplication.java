package com.mahta.backend_gare_routiere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BackendGareRoutiereApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendGareRoutiereApplication.class, args);
    }

}
