package com.traderoperation.shared.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Configurações próprias da aplicação (prefixo {@code trader}). Segredos vêm de variáveis de ambiente. */
@Validated
@ConfigurationProperties(prefix = "trader")
public record TraderProperties(Jwt jwt, Cookie cookie, Cors cors, S3 s3) {

    public record Jwt(
            @NotBlank @Size(min = 64, message = "o segredo JWT precisa ter ao menos 64 caracteres") String secret,
            @Min(1) long accessMinutos,
            @Min(1) long refreshDias) {
    }

    public record Cookie(boolean secure) {
    }

    public record Cors(@NotEmpty List<String> origens) {
    }

    public record S3(@NotBlank String endpoint, @NotBlank String accessKey, @NotBlank String secretKey,
                     @NotBlank String bucket, @NotBlank String region) {
    }
}
