package com.traderoperation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NucleoBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(NucleoBackendApplication.class, args);
    }
}
