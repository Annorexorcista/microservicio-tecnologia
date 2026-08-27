package com.bootcamp.technology.domain.usecase;

import com.bootcamp.technology.domain.exception.TechnologyAlreadyExistsException;
import com.bootcamp.technology.domain.model.Technology;
import com.bootcamp.technology.domain.spi.ITechnologyPersistencePort;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Property-based test (jqwik) del caso de uso {@link TechnologyUseCase}.
 *
 * <p><b>Feature: registrar-tecnologias, Property 2: El nombre duplicado se rechaza
 * sin persistir (comparación case-insensitive)</b>
 *
 * <p>Para todo nombre válido (longitud 1-50 tras {@code trim}) y descripción válida
 * (longitud 1-90 tras {@code trim}), cuando el puerto reporta la existencia del
 * nombre (comparación sin distinción de mayúsculas/minúsculas y con {@code trim}),
 * {@code registerTechnology} emite {@link TechnologyAlreadyExistsException} y nunca
 * invoca {@code save}. El nombre se genera con caracteres alfabéticos (capitalización
 * mixta arbitraria) para reflejar la comparación case-insensitive: el mock retorna
 * {@code true} sin importar el case.
 *
 * <p>Validates: Requirements 2.1, 2.2, 2.3
 */
class TechnologyUseCaseProperty02Test {

    @Property(tries = 100)
    void duplicateNameIsRejectedWithoutPersisting(
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String name,
            @ForAll @AlphaChars @StringLength(min = 1, max = 90) String description) {

        // @AlphaChars garantiza contenido alfabético no vacío (a-z, A-Z, capitalización
        // mixta), por lo que name/description siguen siendo válidos tras trim y quedan
        // dentro de los límites del dominio (1-50 y 1-90).
        ITechnologyPersistencePort persistencePort = mock(ITechnologyPersistencePort.class);
        // El nombre ya existe (case-insensitive): el mock reporta true sin importar el case.
        when(persistencePort.existsByNameIgnoreCase(anyString())).thenReturn(Mono.just(true));

        TechnologyUseCase useCase = new TechnologyUseCase(persistencePort);

        StepVerifier.create(useCase.registerTechnology(new Technology(null, name, description)))
                .expectError(TechnologyAlreadyExistsException.class)
                .verify();

        // Invariante clave: nunca se persiste cuando el nombre está duplicado.
        verify(persistencePort, never()).save(any());
    }
}
