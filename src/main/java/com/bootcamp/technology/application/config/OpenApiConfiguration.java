package com.bootcamp.technology.application.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la documentación OpenAPI del microservicio de tecnologías.
 *
 * <p>Define el bean {@link OpenAPI} con los metadatos de la API (título, versión y
 * descripción). Junto con {@code springdoc-openapi-starter-webflux-ui}, expone la
 * especificación OpenAPI (en {@code /v3/api-docs}) y Swagger UI, describiendo el
 * endpoint de registro de tecnologías. Cubre el Requerimiento 6.1.
 */
@Configuration
public class OpenApiConfiguration {

    private static final String API_TITLE = "Technology Service API";
    private static final String API_VERSION = "1.0.0";
    private static final String API_DESCRIPTION =
            "API del microservicio de tecnologías. Expone el registro de tecnologías, "
                    + "incluyendo el esquema de la solicitud, el esquema de la respuesta y los "
                    + "códigos de estado 201 (creado), 400 (datos inválidos) y 409 (nombre duplicado).";

    /**
     * Bean con los metadatos de la especificación OpenAPI generada por springdoc.
     *
     * @return la definición {@link OpenAPI} con título, versión y descripción del servicio.
     */
    @Bean
    public OpenAPI technologyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(API_TITLE)
                        .version(API_VERSION)
                        .description(API_DESCRIPTION));
    }
}
