package com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.adapter;

import com.bootcamp.technology.domain.exception.TechnologyAlreadyExistsException;
import com.bootcamp.technology.domain.model.Technology;
import com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.entity.TechnologyEntity;
import com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.mapper.TechnologyEntityMapper;
import com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.repository.ITechnologyRepository;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static io.r2dbc.spi.ConnectionFactoryOptions.DATABASE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.HOST;
import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.PORT;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración del {@link TechnologyPersistenceAdapter} contra una base
 * de datos MySQL real levantada con Testcontainers.
 *
 * <p>Verifica el comportamiento del adaptador driven R2DBC de extremo a extremo
 * (adaptador + mapper + repositorio + esquema con restricción UNIQUE), usando
 * {@link StepVerifier} para comprobar los flujos reactivos sin bloquear:
 * <ul>
 *   <li>{@code save} persiste y retorna una {@link Technology} con id asignado
 *       (Req 1.2, 1.3).</li>
 *   <li>{@code existsByNameIgnoreCase} respeta/ignora mayúsculas y minúsculas
 *       (Req 2.1).</li>
 *   <li>Un insert con nombre duplicado (violación de la restricción UNIQUE) se
 *       traduce a {@link TechnologyAlreadyExistsException} (Req 2.3).</li>
 * </ul>
 *
 * <p><b>Requirements: 1.2, 1.3, 2.1, 2.3</b>
 *
 * <p>El contenedor y el {@link ConnectionFactory} se gestionan manualmente (sin
 * contexto Spring Boot completo) para aislar la prueba en la capa de persistencia.
 * El esquema {@code schema.sql} se aplica al contenedor tras el arranque.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TechnologyPersistenceAdapterIntegrationTest {

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
    private static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0")
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

    private ConnectionFactory connectionFactory;
    private ITechnologyRepository repository;
    private TechnologyPersistenceAdapter adapter;

    @BeforeAll
    void startContainer() {
        MYSQL.start();

        this.connectionFactory = ConnectionFactories.get(ConnectionFactoryOptions.builder()
                .option(DRIVER, "mysql")
                .option(HOST, MYSQL.getHost())
                .option(PORT, MYSQL.getMappedPort(MYSQL_PORT))
                .option(USER, DB_USER)
                .option(PASSWORD, DB_PASSWORD)
                .option(DATABASE, DB_NAME)
                .build());

        // Aplica el esquema (tabla technology con la restricción UNIQUE sobre name).
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new org.springframework.core.io.ClassPathResource("schema.sql"));
        populator.populate(connectionFactory).block();

        R2dbcEntityTemplate entityTemplate = new R2dbcEntityTemplate(connectionFactory);
        this.repository = new R2dbcRepositoryFactory(entityTemplate)
                .getRepository(ITechnologyRepository.class);

        R2dbcTransactionManager transactionManager = new R2dbcTransactionManager(connectionFactory);
        TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);

        this.adapter = new TechnologyPersistenceAdapter(
                repository, new TechnologyEntityMapper(), transactionalOperator);
    }

    @AfterAll
    void stopContainer() {
        MYSQL.stop();
    }

    @BeforeEach
    void cleanTable() {
        repository.deleteAll().block();
    }

    // --- save: retorna Technology con id asignado (Req 1.2, 1.3) ---

    @Test
    void save_assignsGeneratedId_andReturnsPersistedTechnology() {
        Technology toSave = new Technology(null, "Java", "Lenguaje de programación");

        StepVerifier.create(adapter.save(toSave))
                .assertNext(saved -> {
                    assertThat(saved.getId()).isNotNull();
                    assertThat(saved.getId()).isPositive();
                    assertThat(saved.getName()).isEqualTo("Java");
                    assertThat(saved.getDescription()).isEqualTo("Lenguaje de programación");
                })
                .verifyComplete();
    }

    @Test
    void save_actuallyPersistsRowInDatabase() {
        Technology toSave = new Technology(null, "Python", "Lenguaje interpretado");

        Long generatedId = adapter.save(toSave).map(Technology::getId).block();
        assertThat(generatedId).isNotNull();

        StepVerifier.create(repository.findById(generatedId))
                .assertNext(entity -> {
                    assertThat(entity.getName()).isEqualTo("Python");
                    assertThat(entity.getDescription()).isEqualTo("Lenguaje interpretado");
                })
                .verifyComplete();
    }

    // --- existsByNameIgnoreCase: respeta/ignora mayúsculas y minúsculas (Req 2.1) ---

    @Test
    void existsByNameIgnoreCase_returnsTrueRegardlessOfCase() {
        adapter.save(new Technology(null, "Spring", "Framework Java")).block();

        StepVerifier.create(adapter.existsByNameIgnoreCase("Spring"))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(adapter.existsByNameIgnoreCase("spring"))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(adapter.existsByNameIgnoreCase("SPRING"))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(adapter.existsByNameIgnoreCase("SpRiNg"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void existsByNameIgnoreCase_returnsFalseWhenNameDoesNotExist() {
        adapter.save(new Technology(null, "Docker", "Contenedores")).block();

        StepVerifier.create(adapter.existsByNameIgnoreCase("Kubernetes"))
                .expectNext(false)
                .verifyComplete();
    }

    // --- insert de nombre duplicado -> TechnologyAlreadyExistsException (Req 2.3) ---

    @Test
    void save_withDuplicateName_isTranslatedToTechnologyAlreadyExistsException() {
        adapter.save(new Technology(null, "React", "Librería UI")).block();

        StepVerifier.create(adapter.save(new Technology(null, "React", "Otra descripción")))
                .expectError(TechnologyAlreadyExistsException.class)
                .verify();
    }

    @Test
    void save_withDuplicateName_doesNotInsertSecondRow() {
        adapter.save(new Technology(null, "Angular", "Framework SPA")).block();

        adapter.save(new Technology(null, "Angular", "Duplicado"))
                .onErrorResume(TechnologyAlreadyExistsException.class, ex -> Mono.empty())
                .block();

        StepVerifier.create(repository.count())
                .expectNext(1L)
                .verifyComplete();
    }
}
