package com.bootcamp.technology.domain.usecase;

import com.bootcamp.technology.domain.model.Technology;
import com.bootcamp.technology.domain.spi.ITechnologyPersistencePort;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test para la propiedad 3 del diseño de la feature registrar-tecnologias.
 *
 * <p><b>Feature: registrar-tecnologias, Property 3: La normalización por {@code trim} es
 * idempotente en los valores persistidos</b>
 *
 * <p>Validates: Requirements 3.3, 4.4
 *
 * <p>Para todo nombre y descripción válidos (longitud 1-50 / 1-90 tras {@code trim}), al
 * rodearlos de una cantidad arbitraria de espacios en blanco iniciales y finales, el
 * {@link Technology} emitido por un registro exitoso tiene {@code name} y {@code description}
 * iguales a los valores sin espacios de borde: {@code trim(pad(x)) == trim(x)}.
 */
class TechnologyUseCaseProperty03Test {

    private static final int NAME_MAX_LENGTH = 50;
    private static final int DESCRIPTION_MAX_LENGTH = 90;

    @Property(tries = 100)
    void trimNormalizationIsIdempotentOnPersistedValues(
            @ForAll("validNames") String baseName,
            @ForAll("validDescriptions") String baseDescription,
            @ForAll("whitespace") String leadingName,
            @ForAll("whitespace") String trailingName,
            @ForAll("whitespace") String leadingDescription,
            @ForAll("whitespace") String trailingDescription) {

        // Arrange: mock del puerto de persistencia (nombre no existe; save hace echo).
        ITechnologyPersistencePort persistencePort = Mockito.mock(ITechnologyPersistencePort.class);
        Mockito.when(persistencePort.existsByNameIgnoreCase(Mockito.anyString()))
                .thenReturn(Mono.just(false));
        Mockito.when(persistencePort.save(Mockito.any(Technology.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Technology.class)));

        TechnologyUseCase useCase = new TechnologyUseCase(persistencePort);

        // Valor válido base rodeado de espacios arbitrarios.
        String paddedName = leadingName + baseName + trailingName;
        String paddedDescription = leadingDescription + baseDescription + trailingDescription;
        Technology input = new Technology(null, paddedName, paddedDescription);

        // Act & Assert: el Technology emitido debe tener name/description == trim(base).
        String expectedName = baseName.trim();
        String expectedDescription = baseDescription.trim();

        StepVerifier.create(useCase.registerTechnology(input))
                .assertNext(emitted -> {
                    // Idempotencia: trim(pad(x)) == trim(x)
                    assertThat(emitted.getName())
                            .isEqualTo(expectedName)
                            .isEqualTo(paddedName.trim());
                    assertThat(emitted.getDescription())
                            .isEqualTo(expectedDescription)
                            .isEqualTo(paddedDescription.trim());
                })
                .verifyComplete();
    }

    /**
     * Nombres válidos: longitud 1-50 tras {@code trim}. Se genera a partir de caracteres
     * no-espacio en los bordes para garantizar que la longitud tras {@code trim} sea la
     * esperada; se permiten espacios internos.
     */
    @Provide
    Arbitrary<String> validNames() {
        return trimmedNonBlank(NAME_MAX_LENGTH);
    }

    /**
     * Descripciones válidas: longitud 1-90 tras {@code trim}.
     */
    @Provide
    Arbitrary<String> validDescriptions() {
        return trimmedNonBlank(DESCRIPTION_MAX_LENGTH);
    }

    /**
     * Genera una cadena cuya forma ya está normalizada (sin espacios de borde), con
     * longitud entre 1 y {@code maxLength}. Los caracteres de borde son no-espacio; los
     * internos pueden ser cualquier carácter imprimible incluido el espacio.
     */
    private Arbitrary<String> trimmedNonBlank(int maxLength) {
        Arbitrary<Character> edgeChars = Arbitraries.chars()
                .alpha().numeric().with('-', '_', '.', '+', '#');
        Arbitrary<String> singleChar = edgeChars.map(String::valueOf);

        Arbitrary<String> multiChar = Combinators.combine(
                        edgeChars,
                        innerString(maxLength - 2),
                        edgeChars)
                .as((first, middle, last) -> "" + first + middle + last);

        // Longitud 1 usa un único carácter no-espacio; longitud >=2 usa borde+medio+borde.
        return Arbitraries.oneOf(singleChar, multiChar)
                .filter(s -> {
                    String trimmed = s.trim();
                    return !trimmed.isEmpty() && trimmed.length() <= maxLength && trimmed.equals(s);
                });
    }

    /**
     * Parte interna (puede incluir espacios) con longitud 0..maxInner.
     */
    private Arbitrary<String> innerString(int maxInner) {
        int bounded = Math.max(0, maxInner);
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withChars(' ', '0', '9', 'A', 'Z')
                .ofMinLength(0)
                .ofMaxLength(bounded);
    }

    /**
     * Espacios en blanco arbitrarios (0..8 caracteres) para rodear los valores base.
     */
    @Provide
    Arbitrary<String> whitespace() {
        return Arbitraries.of(' ', '\t', '\n', '\r', '\f')
                .list().ofMinSize(0).ofMaxSize(8)
                .map(chars -> {
                    StringBuilder sb = new StringBuilder();
                    chars.forEach(sb::append);
                    return sb.toString();
                });
    }
}
