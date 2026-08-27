package com.bootcamp.technology.domain.usecase;

import com.bootcamp.technology.domain.exception.DomainErrorCode;
import com.bootcamp.technology.domain.exception.InvalidTechnologyDataException;
import com.bootcamp.technology.domain.model.Technology;
import com.bootcamp.technology.domain.spi.ITechnologyPersistencePort;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.StringLength;
import org.assertj.core.api.Assertions;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Property-based test (jqwik) del caso de uso {@link TechnologyUseCase}.
 *
 * <p><b>Feature: registrar-tecnologias, Property 5: Descripción vacía u obligatoria
 * es rechazada sin persistir</b>
 *
 * <p>Para toda descripción que tras {@code trim} quede vacía (incluye {@code null},
 * cadena vacía y cualquier combinación de solo espacios en blanco), y con un nombre
 * válido, {@code registerTechnology} emite {@link InvalidTechnologyDataException} con
 * código {@link DomainErrorCode#DESCRIPTION_REQUIRED} y nunca invoca {@code save}.
 *
 * <p>Validates: Requirements 4.1
 */
class TechnologyUseCaseProperty05Test {

    /**
     * Propiedad con descripción vacía o solo espacios: se genera un nombre válido
     * (1-50 tras trim) y una descripción compuesta únicamente de caracteres de
     * espacio en blanco, que tras {@code trim} queda vacía.
     */
    @Property(tries = 100)
    void blankDescriptionIsRejectedWithoutPersisting(
            @ForAll @StringLength(min = 1, max = 50) String rawName,
            @ForAll @StringLength(min = 0, max = 10) String whitespace) {

        String name = normalizeToValidName(rawName);
        // Descripción compuesta solo de espacios en blanco (o vacía) -> vacía tras trim.
        String description = toWhitespaceOnly(whitespace);

        ITechnologyPersistencePort persistencePort = mock(ITechnologyPersistencePort.class);
        TechnologyUseCase useCase = new TechnologyUseCase(persistencePort);

        StepVerifier.create(useCase.registerTechnology(new Technology(null, name, description)))
                .expectErrorSatisfies(error -> {
                    Assertions.assertThat(error).isInstanceOf(InvalidTechnologyDataException.class);
                    Assertions.assertThat(((InvalidTechnologyDataException) error).getCode())
                            .isEqualTo(DomainErrorCode.DESCRIPTION_REQUIRED);
                })
                .verify();

        // Invariante clave: nunca se persiste cuando la descripción es obligatoria/vacía.
        verify(persistencePort, never()).save(any());
    }

    /**
     * Propiedad con descripción {@code null}: se genera un nombre válido y la
     * descripción es {@code null}, que también debe rechazarse como obligatoria.
     */
    @Property(tries = 100)
    void nullDescriptionIsRejectedWithoutPersisting(
            @ForAll @StringLength(min = 1, max = 50) String rawName) {

        String name = normalizeToValidName(rawName);

        ITechnologyPersistencePort persistencePort = mock(ITechnologyPersistencePort.class);
        TechnologyUseCase useCase = new TechnologyUseCase(persistencePort);

        StepVerifier.create(useCase.registerTechnology(new Technology(null, name, null)))
                .expectErrorSatisfies(error -> {
                    Assertions.assertThat(error).isInstanceOf(InvalidTechnologyDataException.class);
                    Assertions.assertThat(((InvalidTechnologyDataException) error).getCode())
                            .isEqualTo(DomainErrorCode.DESCRIPTION_REQUIRED);
                })
                .verify();

        verify(persistencePort, never()).save(any());
    }

    /**
     * Convierte una cadena generada en un nombre válido: reemplaza por 'x' todo
     * carácter que {@link String#trim()} eliminaría (código &lt;= U+0020, que
     * incluye espacios en blanco y caracteres de control), asegurando así que el
     * nombre no quede vacío tras {@code trim}, y recorta a la longitud máxima
     * permitida por el dominio (50).
     */
    private static String normalizeToValidName(String raw) {
        StringBuilder cleaned = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            cleaned.append(c <= ' ' ? 'x' : c);
        }
        if (cleaned.length() == 0) {
            cleaned.append('x');
        }
        if (cleaned.length() > 50) {
            cleaned.setLength(50);
        }
        return cleaned.toString();
    }

    /**
     * Produce una cadena compuesta únicamente de caracteres de espacio en blanco a
     * partir de una longitud arbitraria. El resultado (posiblemente vacío) queda
     * vacío tras aplicar {@code trim}.
     */
    private static String toWhitespaceOnly(String raw) {
        char[] whitespaceChars = {' ', '\t', '\n', '\r', '\f'};
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            builder.append(whitespaceChars[i % whitespaceChars.length]);
        }
        return builder.toString();
    }
}
