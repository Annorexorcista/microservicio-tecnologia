package com.bootcamp.technology.infrastructure.adapters.driving.webflux;

import com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.repository.ITechnologyRepository;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.dto.ErrorResponse;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.dto.TechnologyRequest;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.dto.TechnologyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración end-to-end del endpoint {@code POST /api/v1/technologies}
 * usando {@link WebTestClient} contra el contexto completo de Spring Boot y una
 * base de datos MySQL real levantada con Testcontainers.
 *
 * <p>Ejercita el flujo reactivo completo de la arquitectura hexagonal sin ningún
 * mock: {@code TechnologyRouter -> TechnologyHandler -> TechnologyUseCase ->
 * TechnologyPersistenceAdapter -> MySQL}, y la traducción de errores del
 * {@code GlobalErrorWebExceptionHandler}.
 *
 * <p>Casos cubiertos:
 * <ul>
 *   <li>POST válido -&gt; {@code 201 Created} con {@link TechnologyResponse} correcto (Req 1.3).</li>
 *   <li>Nombre duplicado -&gt; {@code 409 Conflict} con {@link ErrorResponse} (Req 2.3).</li>
 *   <li>Nombre vacío / null -&gt; {@code 400 Bad Request} (Req 3.1).</li>
 *   <li>Nombre de 51 caracteres -&gt; {@code 400 Bad Request} (Req 3.2).</li>
 *   <li>Descripción vacía -&gt; {@code 400 Bad Request} (Req 4.1).</li>
 *   <li>Descripción de 91 caracteres -&gt; {@code 400 Bad Request} (Req 4.2).</li>
 * </ul>
 *
 * <p><b>Requirements: 1.3, 2.3, 3.1, 3.2, 4.1, 4.2</b>
 *
 * <p>El contenedor MySQL se enlaza al contexto reactivo mediante
 * {@link DynamicPropertySource}, sobreescribiendo las propiedades
 * {@code spring.r2dbc.*} para que R2DBC apunte al contenedor. La inicialización
 * del esquema ({@code schema.sql}) la realiza Spring Boot al arrancar.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TechnologyEndpointIntegrationTest {

    private static final int MYSQL_PORT = 3306;
    private static final String DB_NAME = "technology_db";
    private static final String DB_USER = "test";
    private static final String DB_PASSWORD = "test";

    // Este proyecto es puramente R2DBC y NO tiene el driver JDBC de MySQL en el classpath
    // de test. {@code MySQLContainer} (un {@code JdbcDatabaseContainer}) siempre verifica el
    // arranque abriendo una conexión JDBC ("SELECT 1") en {@code waitUntilContainerStarted()},
    // ignorando cualquier wait strategy personalizada; eso provoca
    // ClassNotFoundException: com.mysql.jdbc.Driver.
    //
    // Por eso arrancamos MySQL con un {@code GenericContainer} genérico configurado por
    // variables de entorno y una wait strategy basada en el log de arranque de MySQL. Así
    // levantamos un MySQL real sin depender de ningún driver JDBC (Opción A).
    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0")
            .withEnv("MYSQL_DATABASE", DB_NAME)
            .withEnv("MYSQL_USER", DB_USER)
            .withEnv("MYSQL_PASSWORD", DB_PASSWORD)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withExposedPorts(MYSQL_PORT)
            // Espera al arranque DEFINITIVO de MySQL. El mensaje final incluye el puerto de red
            // (port: 3306) y solo aparece cuando el servidor acepta conexiones de forma estable,
            // evitando conectar durante la fase de init temporal (que cierra la conexión).
            .waitingFor(Wait.forLogMessage(".*port: 3306  MySQL Community Server.*", 1)
                    .withStartupTimeout(Duration.ofSeconds(180)));

    @DynamicPropertySource
    static void registerR2dbcProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> String.format(
                "r2dbc:mysql://%s:%d/%s",
                MYSQL.getHost(),
                MYSQL.getMappedPort(MYSQL_PORT),
                DB_NAME));
        registry.add("spring.r2dbc.username", () -> DB_USER);
        registry.add("spring.r2dbc.password", () -> DB_PASSWORD);
        // schema.sql se ejecuta sobre la misma conexión R2DBC (mode: always).
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ITechnologyRepository repository;

    @BeforeEach
    void cleanTable() {
        repository.deleteAll().block();
    }

    // --- POST válido -> 201 con TechnologyResponse correcto (Req 1.3) ---

    @Test
    void post_withValidData_returns201WithTechnologyResponse() {
        TechnologyRequest request = new TechnologyRequest("Java", "Lenguaje de programación");

        webTestClient.post()
                .uri("/api/v1/technologies")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(TechnologyResponse.class)
                .value(response -> {
                    assertThat(response.id()).isNotNull();
                    assertThat(response.id()).isPositive();
                    assertThat(response.name()).isEqualTo("Java");
                    assertThat(response.description()).isEqualTo("Lenguaje de programación");
                });
    }

    @Test
    void post_withSurroundingWhitespace_returns201WithTrimmedValues() {
        TechnologyRequest request = new TechnologyRequest("  Python  ", "  Lenguaje interpretado  ");

        webTestClient.post()
                .uri("/api/v1/technologies")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TechnologyResponse.class)
                .value(response -> {
                    assertThat(response.name()).isEqualTo("Python");
                    assertThat(response.description()).isEqualTo("Lenguaje interpretado");
                });
    }

    // --- Nombre duplicado -> 409 con ErrorResponse (Req 2.3) ---

    @Test
    void post_withDuplicateName_returns409WithErrorResponse() {
        TechnologyRequest first = new TechnologyRequest("Spring", "Framework Java");

        webTestClient.post()
                .uri("/api/v1/technologies")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(first)
                .exchange()
                .expectStatus().isCreated();

        // El mismo nombre con distinta capitalización también debe rechazarse (case-insensitive).
        TechnologyRequest duplicate = new TechnologyRequest("spring", "Otra descripción");

        webTestClient.post()
                .uri("/api/v1/technologies")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(duplicate)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(ErrorResponse.class)
                .value(error -> {
                    assertThat(error.status()).isEqualTo(HttpStatus.CONFLICT.value());
                    assertThat(error.message()).isNotBlank();
                });
    }

    // --- Nombre vacío / null -> 400 (Req 3.1) ---

    @Test
    void post_withEmptyName_returns400() {
        TechnologyRequest request = new TechnologyRequest("   ", "Descripción válida");

        webTestClient.post()
                .uri("/api/v1/technologies")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(error -> assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    void post_withNullName_returns400() {
        TechnologyRequest request = new TechnologyRequest(null, "Descripción válida");

        webTestClient.post()
                .uri("/api/v1/technologies")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(error -> assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    // --- Nombre de 51 caracteres -> 400 (Req 3.2) ---

    @Test
    void post_withNameExceeding50Chars_returns400() {
        String name51 = "a".repeat(51);
        TechnologyRequest request = new TechnologyRequest(name51, "Descripción válida");

        webTestClient.post()
                .uri("/api/v1/technologies")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(error -> assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    // --- Descripción vacía -> 400 (Req 4.1) ---

    @Test
    void post_withEmptyDescription_returns400() {
        TechnologyRequest request = new TechnologyRequest("Docker", "   ");

        webTestClient.post()
                .uri("/api/v1/technologies")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(error -> assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    // --- Descripción de 91 caracteres -> 400 (Req 4.2) ---

    @Test
    void post_withDescriptionExceeding90Chars_returns400() {
        String description91 = "d".repeat(91);
        TechnologyRequest request = new TechnologyRequest("Kubernetes", description91);

        webTestClient.post()
                .uri("/api/v1/technologies")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(error -> assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }
}
