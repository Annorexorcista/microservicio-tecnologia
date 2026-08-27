package com.bootcamp.technology.domain.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bootcamp.technology.domain.exception.DomainErrorCode;
import com.bootcamp.technology.domain.exception.InvalidTechnologyDataException;
import com.bootcamp.technology.domain.model.Technology;
import com.bootcamp.technology.domain.spi.ITechnologyPersistencePort;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.assertj.core.api.Assertions;
import reactor.test.StepVerifier;

/**
 * Property-based test (jqwik) del caso de uso {@link TechnologyUseCase}.
 *
 * <p><b>Feature: registrar-tecnologias, Property 6: Nombre que excede 50
 * caracteres es rechazado sin persistir</b>
 *
 * <p>Para todo nombre cuya longitud tras {@code trim} sea mayor a 50, y con una
 * descripción válida, {@code registerTechnology} emite
 * {@link InvalidTechnologyDataException} con código
 * {@link DomainErrorCode#NAME_TOO_LONG} y nunca invoca {@code save}.
 *
 * <p><b>Validates: Requirements 3.2, 3.4</b>
 */
class TechnologyUseCaseProperty06Test {

    /**
     * Feature: registrar-tecnologias, Property 6: Nombre que excede 50 caracteres
     * es rechazado sin persistir.
     *
     * <p>Se genera un nombre cuya longitud tras {@code trim} es estrictamente mayor
     * a 50 (&gt;= 51), posiblemente rodeado de espacios en blanco de borde (que no
     * cuentan para la longitud tras {@code trim}), junto con una descripción válida
     * (1-90 tras {@code trim}). El orden de validación del usecase (nombre-requerido,
     * nombre-demasiado-largo, descripción-requerida, descripción-demasiado-larga)
     * garantiza que con nombre no vacío y descripción válida se alcance la rama
     * {@code NAME_TOO_LONG}.
     *
     * <p><b>Validates: Requirements 3.2, 3.4</b>
     */
    @Property(tries = 100)
    void nameLongerThan50IsRejectedWithoutPersisting(
            @ForAll("tooLongNames") String name,
            @ForAll("validDescriptions") String description) {

        ITechnologyPersistencePort persistencePort = mock(ITechnologyPersistencePort.class);
        TechnologyUseCase useCase = new TechnologyUseCase(persistencePort);

        Technology input = new Technology(null, name, description);

        StepVerifier.create(useCase.registerTechnology(input))
                .expectErrorSatisfies(error -> {
                    Assertions.assertThat(error)
                            .isInstanceOf(InvalidTechnologyDataException.class);
                    Assertions.assertThat(((InvalidTechnologyDataException) error).getCode())
                            .isEqualTo(DomainErrorCode.NAME_TOO_LONG);
                })
                .verify();

        // Invariante clave: nunca se persiste cuando el nombre excede la longitud máxima.
        verify(persistencePort, never()).save(any());
    }

    /**
     * Genera nombres cuya longitud tras {@code trim} es estrictamente mayor a 50.
     *
     * <p>El núcleo del nombre se compone de caracteres imprimibles no-espacio
     * (que {@code trim} no elimina) con longitud entre 51 y 120. Opcionalmente se
     * rodea de espacios en blanco de borde para verificar que dichos espacios NO
     * cuentan hacia la longitud validada: aun así el núcleo mantiene &gt; 50 tras
     * {@code trim}.
     */
    @Provide
    Arbitrary<String> tooLongNames() {
        Arbitrary<String> core = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withChars('A', 'Z', '0', '9')
                .ofMinLength(51)
                .ofMaxLength(120);

        return Combinators.combine(whitespacePadding(), core, whitespacePadding())
                .as((lead, coreValue, trail) -> lead + coreValue + trail);
    }

    /**
     * Genera cadenas compuestas únicamente de caracteres de espacio en blanco
     * (posiblemente vacías) para usarlas como relleno de borde del nombre.
     */
    private Arbitrary<String> whitespacePadding() {
        return Arbitraries.of(" ", "\t", "\n", "\r", "\f", "\u000B")
                .list().ofMinSize(0).ofMaxSize(8)
                .map(chars -> String.join("", chars));
    }

    /**
     * Genera descripciones válidas: entre 1 y 90 caracteres imprimibles no-espacio,
     * de modo que sean siempre válidas tras {@code trim} y la validación aísle la
     * regla del nombre.
     */
    @Provide
    Arbitrary<String> validDescriptions() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withChars('A', 'Z', '0', '9')
                .ofMinLength(1)
                .ofMaxLength(90);
    }
}
