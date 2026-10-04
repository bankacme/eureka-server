package com.bank.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Registro y descubrimiento de servicios (RNF-16). Sin dominio ni base de datos: el registro vive
 * en memoria y toda la configuración llega de {@code eureka-server.yml} en el Config Server.
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
