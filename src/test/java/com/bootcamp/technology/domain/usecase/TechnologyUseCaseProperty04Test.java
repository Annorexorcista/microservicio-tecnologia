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
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.assertj.core.api.Assertions;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Property-based test para el {@code TechnologyUseCase}.
 *
 * <p>Feature: registrar-tecnologias, Property 4: Nombre vacío u obligatorio es
 * rechazado sin persistir.
 *
 * <p>Para toda cadena que tras {@code trim} quede vacía (incluye {@code null},
 * cadena vacía y cualquier combinación de solo espacios en blanco),
 * {@code registerTechnology} emite {@link InvalidTechnologyDataException} con
 * código {@link DomainErrorCode#NAME_REQUIRED} y nunca invoca {@code save}.
 *
 * <p>**Validates: Requirements 3.1, 3.4**
 */
class TechnologyUseCaseProperty04Test {

    /**
     * Feature: registrar-tecnologias, Property 4: Nombre vacío u obligatorio es
     * rechazado sin persistir.
     *
     * **Validates: Requirements 3.1, 3.4**
     */
    @Property(tries = 100)
    void blankOrMissingNameIsRejectedWithoutPersisting(
            @ForAll("blankNames") String blankName,
            @ForAll("validDescriptions") String description) {

        ITechnologyPersistencePort persistencePort = mock(ITechnologyPersistencePort.class);
        TechnologyUseCase useCase = new TechnologyUseCase(persistencePort);

        Technology input = new Technology(null, blankName, description);

        StepVerifier.create(useCase.registerTechnology(input))
                .expectErrorSatisfies(error -> {
                    Assertions.assertThat(error)
                            .isInstanceOf(InvalidTechnologyDataException.class);
                    Assertions.assertThat(((InvalidTechnologyDataException) error).getCode())
                            .isEqualTo(DomainErrorCode.NAME_REQUIRED);
                })
                .verify();

        verify(persistencePort, never()).save(any());
    }

    /**
     * Genera nombres que tras {@code trim} quedan vacíos: {@code null}, cadena
     * vacía y cadenas compuestas únicamente por caracteres de espacio en blanco
     * (espacios, tabuladores, saltos de línea).
     */
    @Provide
    Arbitrary<String> blankNames() {
        Arbitrary<String> whitespaceOnly = Arbitraries.of(" ", "\t", "\n", "\r", "\f", "\u000B")
                .list().ofMinSize(0).ofMaxSize(10)
                .map(chars -> String.join("", chars));
        // Incluye null, cadena vacía y solo-espacios (todos quedan vacíos tras trim).
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(""),
                whitespaceOnly);
    }

    /**
     * Genera descripciones válidas: entre 1 y 90 caracteres tras {@code trim}.
     * Se usan caracteres imprimibles no-espacio para garantizar que la
     * descripción sea siempre válida y aislar la validación del nombre.
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
