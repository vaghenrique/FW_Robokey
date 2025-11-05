package com.robokey.fw.config; // Um novo sub-pacote 'config'

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Classe de configuração para o Swagger (OpenAPI).
 * Isto define o título, descrição e versão da sua API
 * que aparecerá no topo da página do Swagger.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Robokey FW")
                        .version("1.0.0")
                        .description("API para controlar a máquina Robokey via USB/Serial.")
                );
    }
}
