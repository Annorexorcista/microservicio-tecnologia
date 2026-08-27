package com.bootcamp.technology.domain.usecase;

import com.bootcamp.technology.domain.exception.DomainErrorCode;
import com.bootcamp.technology.domain.exception.InvalidTechnologyDataException;
import com.bootcamp.technology.domain.exception.TechnologyAlreadyExistsException;
import com.bootcamp.technology.domain.model.Technology;
import com.bootcamp.technology.domain.spi.ITechnologyPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios (JUnit 5 + Mockito + StepVerifier) del caso de uso
 * {@link TechnologyUseCase}. Cubren ejemplos y casos borde de las reglas de
 * negocio: obligatoriedad y longitud de nombre/descripción, unicidad del nombre,
 * normalización por {@code trim} y la invariante de no persistir ante errores.
 *
 * <p>El puerto de persistencia {@link ITechnologyPersistencePort} se mockea con
 * Mockito; los flujos reactivos se verifican con {@link StepVerifier}.
 */
@ExtendWith(MockitoExtension.class)
class TechnologyUseCaseTest {

    @Mock
    private ITechnologyPersistencePort persistencePort;

    @InjectMocks
    private TechnologyUseCase useCase;

    private static final String VALID_NAME = "Java";
    private static final String VALID_DESCRIPTION = "Lenguaje de programación orientado a objetos";

    @BeforeEach
    void setUp() {
        useCase = new TechnologyUseCase(persistencePort);
    }

    private Technology input(String name, String description) {
        return new Technology(null, name, description);
    }

    @Nested
    @DisplayName("Registro válido")
    class ValidRegistration {

        @Test
        @DisplayName("emite Technology persistido cuando nombre y descripción son válidos y el nombre no existe")
        void registersValidTechnology() {
            when(persistencePort.existsByNameIgnoreCase(anyString())).thenReturn(Mono.just(false));
            when(persistencePort.save(any(Technology.class)))
                    .thenAnswer(invocation -> {
                        Technology t = invocation.getArgument(0);
                        return Mono.just(new Technology(1L, t.getName(), t.getDescription()));
                    });

            StepVerifier.create(useCase.registerTechnology(input(VALID_NAME, VALID_DESCRIPTION)))
                    .assertNext(saved -> {
                        assertThat(saved.getId()).isEqualTo(1L);
                        assertThat(saved.getName()).isEqualTo(VALID_NAME);
                        assertThat(saved.getDescription()).isEqualTo(VALID_DESCRIPTION);
                    })
                    .verifyComplete();

            verify(persistencePort, times(1)).save(any(Technology.class));
        }

        @Test
        @DisplayName("acepta nombre de 1 carácter y descripción de 1 carácter (límites mínimos)")
        void acceptsMinimumLengths() {
            when(persistencePort.existsByNameIgnoreCase(anyString())).thenReturn(Mono.just(false));
            when(persistencePort.save(any(Technology.class)))
                    .thenAnswer(invocation -> Mono.just((Technology) invocation.getArgument(0)));

            StepVerifier.create(useCase.registerTechnology(input("A", "D")))
                    .assertNext(saved -> {
                        assertThat(saved.getName()).isEqualTo("A");
                        assertThat(saved.getDescription()).isEqualTo("D");
                    })
                    .verifyComplete();

            verify(persistencePort, times(1)).save(any(Technology.class));
        }

        @Test
        @DisplayName("acepta nombre de 50 caracteres y descripción de 90 caracteres (límites máximos)")
        void acceptsMaximumLengths() {
            String name50 = "N".repeat(50);
            String description90 = "D".repeat(90);
            when(persistencePort.existsByNameIgnoreCase(anyString())).thenReturn(Mono.just(false));
            when(persistencePort.save(any(Technology.class)))
                    .thenAnswer(invocation -> Mono.just((Technology) invocation.getArgument(0)));

            StepVerifier.create(useCase.registerTechnology(input(name50, description90)))
                    .assertNext(saved -> {
                        assertThat(saved.getName()).isEqualTo(name50);
                        assertThat(saved.getDescription()).isEqualTo(description90);
                    })
                    .verifyComplete();

            verify(persistencePort, times(1)).save(any(Technology.class));
        }

        @Test
        @DisplayName("normaliza (trim) nombre y descripción en el Technology emitido y persistido")
        void trimsNameAndDescription() {
            when(persistencePort.existsByNameIgnoreCase(anyString())).thenReturn(Mono.just(false));
            when(persistencePort.save(any(Technology.class)))
                    .thenAnswer(invocation -> Mono.just((Technology) invocation.getArgument(0)));

            StepVerifier.create(useCase.registerTechnology(input("   Java   ", "   Descripción con espacios   ")))
                    .assertNext(saved -> {
                        assertThat(saved.getName()).isEqualTo("Java");
                        assertThat(saved.getDescription()).isEqualTo("Descripción con espacios");
                    })
                    .verifyComplete();

            ArgumentCaptor<Technology> captor = ArgumentCaptor.forClass(Technology.class);
            verify(persistencePort).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Java");
            assertThat(captor.getValue().getDescription()).isEqualTo("Descripción con espacios");
        }
    }

    @Nested
    @DisplayName("Validación del nombre")
    class NameValidation {

        @ParameterizedTest(name = "nombre inválido [{0}] -> NAME_REQUIRED")
        @NullSource
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("nombre null / vacío / solo espacios -> InvalidTechnologyDataException NAME_REQUIRED sin persistir")
        void rejectsBlankName(String invalidName) {
            StepVerifier.create(useCase.registerTechnology(input(invalidName, VALID_DESCRIPTION)))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(InvalidTechnologyDataException.class);
                        assertThat(((InvalidTechnologyDataException) error).getCode())
                                .isEqualTo(DomainErrorCode.NAME_REQUIRED);
                    })
                    .verify();

            verify(persistencePort, never()).save(any());
        }

        @Test
        @DisplayName("nombre de 51 caracteres -> InvalidTechnologyDataException NAME_TOO_LONG sin persistir")
        void rejectsNameTooLong() {
            String name51 = "N".repeat(51);

            StepVerifier.create(useCase.registerTechnology(input(name51, VALID_DESCRIPTION)))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(InvalidTechnologyDataException.class);
                        assertThat(((InvalidTechnologyDataException) error).getCode())
                                .isEqualTo(DomainErrorCode.NAME_TOO_LONG);
                    })
                    .verify();

            verify(persistencePort, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Validación de la descripción")
    class DescriptionValidation {

        @ParameterizedTest(name = "descripción inválida [{0}] -> DESCRIPTION_REQUIRED")
        @NullSource
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("descripción null / vacía / solo espacios -> InvalidTechnologyDataException DESCRIPTION_REQUIRED sin persistir")
        void rejectsBlankDescription(String invalidDescription) {
            StepVerifier.create(useCase.registerTechnology(input(VALID_NAME, invalidDescription)))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(InvalidTechnologyDataException.class);
                        assertThat(((InvalidTechnologyDataException) error).getCode())
                                .isEqualTo(DomainErrorCode.DESCRIPTION_REQUIRED);
                    })
                    .verify();

            verify(persistencePort, never()).save(any());
        }

        @Test
        @DisplayName("descripción de 91 caracteres -> InvalidTechnologyDataException DESCRIPTION_TOO_LONG sin persistir")
        void rejectsDescriptionTooLong() {
            String description91 = "D".repeat(91);

            StepVerifier.create(useCase.registerTechnology(input(VALID_NAME, description91)))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(InvalidTechnologyDataException.class);
                        assertThat(((InvalidTechnologyDataException) error).getCode())
                                .isEqualTo(DomainErrorCode.DESCRIPTION_TOO_LONG);
                    })
                    .verify();

            verify(persistencePort, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Unicidad del nombre")
    class NameUniqueness {

        @Test
        @DisplayName("nombre duplicado (existsByNameIgnoreCase -> true) -> TechnologyAlreadyExistsException sin persistir")
        void rejectsDuplicateName() {
            when(persistencePort.existsByNameIgnoreCase(anyString())).thenReturn(Mono.just(true));

            StepVerifier.create(useCase.registerTechnology(input(VALID_NAME, VALID_DESCRIPTION)))
                    .expectError(TechnologyAlreadyExistsException.class)
                    .verify();

            verify(persistencePort, never()).save(any());
        }
    }
}
