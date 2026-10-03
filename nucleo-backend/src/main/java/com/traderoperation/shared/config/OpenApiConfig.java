package com.traderoperation.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI traderOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Trader Operation — API do núcleo")
                .version("0.1.0")
                .description("Autenticação por cookie httpOnly: chame GET /api/auth/csrf, depois POST /api/auth/login "
                        + "enviando o cabeçalho X-XSRF-TOKEN com o valor do cookie XSRF-TOKEN."));
    }
}
