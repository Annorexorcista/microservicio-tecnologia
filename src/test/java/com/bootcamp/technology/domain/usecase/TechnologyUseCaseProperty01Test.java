package com.bootcamp.technology.domain.usecase;

import com.bootcamp.technology.domain.model.Technology;
import com.bootcamp.technology.domain.spi.ITechnologyPersistencePort;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Feature: registrar-tecnologias, Property 1: Registro válido conserva datos
 * normalizados y persiste.
 *
 * <p>Para todo nombre válido (longitud 1-50 tras {@code trim}) y toda descripción
 * válida (longitud 1-90 tras {@code trim}), cuando el puerto de persistencia
 * indica que el nombre no existe, {@code registerTechnology} emite un
 * {@link Technology} cuyo {@code name} y {@code description} son los valores
 * normalizados (con {@code trim} aplicado) e invoca {@code save} exactamente
 * una vez.
 *
 * <p><b>Validates: Requirements 1.1, 2.4, 3.3, 4.3</b>
 */
class TechnologyUseCaseProperty01Test {

    @Property(tries = 100)
    void validRegistrationKeepsNormalizedDataAndPersists(
            @ForAll("paddedValidNames") String rawName,
            @ForAll("paddedValidDescriptions") String rawDescription) {

        ITechnologyPersistencePort persistencePort = mock(ITechnologyPersistencePort.class);
        when(persistencePort.existsByNameIgnoreCase(anyString())).thenReturn(Mono.just(false));
        when(persistencePort.save(any(Technology.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Technology.class)));

        TechnologyUseCase useCase = new TechnologyUseCase(persistencePort);

        String expectedName = rawName.trim();
        String expectedDescription = rawDescription.trim();

        StepVerifier.create(useCase.registerTechnology(new Technology(null, rawName, rawDescription)))
                .assertNext(technology -> {
                    org.assertj.core.api.Assertions.assertThat(technology.getName())
                            .isEqualTo(expectedName);
                    org.assertj.core.api.Assertions.assertThat(technology.getDescription())
                            .isEqualTo(expectedDescription);
                })
                .verifyComplete();

        ArgumentCaptor<Technology> savedCaptor = ArgumentCaptor.forClass(Technology.class);
        verify(persistencePort, times(1)).save(savedCaptor.capture());
        Technology saved = savedCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(saved.getName()).isEqualTo(expectedName);
        org.assertj.core.api.Assertions.assertThat(saved.getDescription()).isEqualTo(expectedDescription);
    }

    /**
     * Genera nombres cuyo contenido tras {@code trim} tiene entre 1 y 50
     * caracteres, opcionalmente rodeados de espacios en blanco iniciales/finales.
     * El núcleo se construye con caracteres no-espacio en los bordes para que el
     * {@code trim} recorte exactamente el padding y no parte del contenido.
     */
    @Provide
    Arbitrary<String> paddedValidNames() {
        return paddedValidText(50);
    }

    /**
     * Genera descripciones cuyo contenido tras {@code trim} tiene entre 1 y 90
     * caracteres, opcionalmente rodeadas de espacios en blanco.
     */
    @Provide
    Arbitrary<String> paddedValidDescriptions() {
        return paddedValidText(90);
    }

    private Arbitrary<String> paddedValidText(int maxCoreLength) {
        Arbitrary<String> core = validCore(maxCoreLength);
        Arbitrary<String> leftPad = whitespace();
        Arbitrary<String> rightPad = whitespace();
        return Combinators.combine(leftPad, core, rightPad)
                .as((left, c, right) -> left + c + right);
    }

    /**
     * Construye un núcleo válido: longitud entre 1 y {@code maxCoreLength}, sin
     * espacios en los extremos (garantiza que {@code trim(core) == core}).
     */
    private Arbitrary<String> validCore(int maxCoreLength) {
        // Caracteres visibles (no espacios en blanco) para los bordes del núcleo.
        Arbitrary<Character> edgeChar = Arbitraries.chars()
                .with('a', 'b', 'c', 'X', 'Y', 'Z', '0', '9', '-', '_', 'ñ');
        // El interior puede contener espacios internos sin afectar el trim.
        Arbitrary<String> interior = Arbitraries.strings()
                .withChars("abcABC 0123ñ-_")
                .ofMinLength(0)
                .ofMaxLength(Math.max(0, maxCoreLength - 2));

        return Combinators.combine(edgeChar, interior, edgeChar)
                .as((first, mid, last) -> {
                    String candidate = first + mid + last;
                    if (candidate.length() > maxCoreLength) {
                        candidate = candidate.substring(0, maxCoreLength);
                        // Garantiza que el último carácter no sea espacio tras el recorte.
                        if (candidate.endsWith(" ")) {
                            candidate = candidate.substring(0, candidate.length() - 1) + first;
                        }
                    }
                    return candidate;
                })
                .filter(s -> {
                    String trimmed = s.trim();
                    return trimmed.length() >= 1
                            && trimmed.length() <= maxCoreLength
                            && trimmed.equals(s);
                });
    }

    private Arbitrary<String> whitespace() {
        return Arbitraries.strings().withChars(' ', '\t').ofMinLength(0).ofMaxLength(4);
    }
}
