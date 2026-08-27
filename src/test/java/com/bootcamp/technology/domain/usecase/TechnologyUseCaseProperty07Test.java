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
 * <p><b>Feature: registrar-tecnologias, Property 7: Descripción que excede 90
 * caracteres es rechazada sin persistir</b>
 *
 * <p>Para toda descripción cuya longitud tras {@code trim} sea mayor a 90, y con
 * un nombre válido, {@code registerTechnology} emite
 * {@link InvalidTechnologyDataException} con código
 * {@link DomainErrorCode#DESCRIPTION_TOO_LONG} y nunca invoca {@code save}.
 *
 * <p><b>Validates: Requirements 4.2</b>
 */
class TechnologyUseCaseProperty07Test {

    /**
     * Feature: registrar-tecnologias, Property 7: Descripción que excede 90
     * caracteres es rechazada sin persistir.
     *
     * <p>Se genera una descripción cuya longitud tras {@code trim} es estrictamente
     * mayor a 90 (&gt;= 91), posiblemente rodeada de espacios en blanco de borde
     * (que no cuentan para la longitud tras {@code trim}), junto con un nombre
     * válido (1-50 tras {@code trim}). El orden de validación del usecase
     * (nombre-requerido, nombre-demasiado-largo, descripción-requerida,
     * descripción-demasiado-larga) garantiza que con un nombre válido y una
     * descripción no vacía sobredimensionada se alcance la rama
     * {@code DESCRIPTION_TOO_LONG}.
     *
     * <p><b>Validates: Requirements 4.2</b>
     */
    @Property(tries = 100)
    void descriptionLongerThan90IsRejectedWithoutPersisting(
            @ForAll("validNames") String name,
            @ForAll("tooLongDescriptions") String description) {

        ITechnologyPersistencePort persistencePort = mock(ITechnologyPersistencePort.class);
        TechnologyUseCase useCase = new TechnologyUseCase(persistencePort);

        Technology input = new Technology(null, name, description);

        StepVerifier.create(useCase.registerTechnology(input))
                .expectErrorSatisfies(error -> {
                    Assertions.assertThat(error)
                            .isInstanceOf(InvalidTechnologyDataException.class);
                    Assertions.assertThat(((InvalidTechnologyDataException) error).getCode())
                            .isEqualTo(DomainErrorCode.DESCRIPTION_TOO_LONG);
                })
                .verify();

        // Invariante clave: nunca se persiste cuando la descripción excede la longitud máxima.
        verify(persistencePort, never()).save(any());
    }

    /**
     * Genera nombres válidos: entre 1 y 50 caracteres imprimibles no-espacio, de
     * modo que sean siempre válidos tras {@code trim} y la validación aísle la
     * regla de la descripción.
     */
    @Provide
    Arbitrary<String> validNames() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withChars('A', 'Z', '0', '9')
                .ofMinLength(1)
                .ofMaxLength(50);
    }

    /**
     * Genera descripciones cuya longitud tras {@code trim} es estrictamente mayor
     * a 90.
     *
     * <p>El núcleo de la descripción se compone de caracteres imprimibles
     * no-espacio (que {@code trim} no elimina) con longitud entre 91 y 160.
     * Opcionalmente se rodea de espacios en blanco de borde para verificar que
     * dichos espacios NO cuentan hacia la longitud validada: aun así el núcleo
     * mantiene &gt; 90 tras {@code trim}.
     */
    @Provide
    Arbitrary<String> tooLongDescriptions() {
        Arbitrary<String> core = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withChars('A', 'Z', '0', '9')
                .ofMinLength(91)
                .ofMaxLength(160);

        return Combinators.combine(whitespacePadding(), core, whitespacePadding())
                .as((lead, coreValue, trail) -> lead + coreValue + trail);
    }

    /**
     * Genera cadenas compuestas únicamente de caracteres de espacio en blanco
     * (posiblemente vacías) para usarlas como relleno de borde de la descripción.
     */
    private Arbitrary<String> whitespacePadding() {
        return Arbitraries.of(" ", "\t", "\n", "\r", "\f", "\u000B")
                .list().ofMinSize(0).ofMaxSize(8)
                .map(chars -> String.join("", chars));
    }
}
