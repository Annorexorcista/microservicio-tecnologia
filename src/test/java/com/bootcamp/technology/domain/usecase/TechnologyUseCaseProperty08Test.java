package com.bootcamp.technology.domain.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bootcamp.technology.domain.exception.InvalidTechnologyDataException;
import com.bootcamp.technology.domain.exception.TechnologyAlreadyExistsException;
import com.bootcamp.technology.domain.model.Technology;
import com.bootcamp.technology.domain.spi.ITechnologyPersistencePort;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.assertj.core.api.Assertions;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Property-based test (jqwik) del caso de uso {@link TechnologyUseCase}.
 *
 * <p><b>Feature: registrar-tecnologias, Property 8: Invariante de no persistencia
 * ante cualquier error de validación o unicidad</b>
 *
 * <p>Para toda solicitud de registro que falle cualquier validación de negocio
 * (obligatoriedad, longitud) o la verificación de unicidad,
 * {@code registerTechnology} termina con un error y el puerto de persistencia
 * {@code save} no es invocado en ningún caso (invariante de no persistencia).
 *
 * <p><b>Validates: Requirements 2.2, 3.4, 4.1, 4.2</b>
 */
class TechnologyUseCaseProperty08Test {

    /** Longitud máxima válida del nombre tras {@code trim}. */
    private static final int NAME_MAX_LENGTH = 50;

    /** Longitud máxima válida de la descripción tras {@code trim}. */
    private static final int DESCRIPTION_MAX_LENGTH = 90;

    /**
     * Categorías de rechazo cubiertas por el espacio de generación. Cada escenario
     * generado pertenece a exactamente una de estas categorías y debe producir un
     * error terminal sin que {@code save} sea invocado.
     */
    private enum RejectionCategory {
        NAME_REQUIRED,
        NAME_TOO_LONG,
        DESCRIPTION_REQUIRED,
        DESCRIPTION_TOO_LONG,
        DUPLICATE_NAME
    }

    /**
     * Escenario de rechazo: agrupa los campos crudos de entrada, la categoría a la
     * que pertenece y si el puerto debe reportar existencia del nombre.
     */
    private static final class RejectionScenario {
        final String name;
        final String description;
        final RejectionCategory category;
        final boolean nameExists;

        RejectionScenario(String name, String description, RejectionCategory category, boolean nameExists) {
            this.name = name;
            this.description = description;
            this.category = category;
            this.nameExists = nameExists;
        }
    }

    /**
     * Feature: registrar-tecnologias, Property 8: Invariante de no persistencia
     * ante cualquier error de validación o unicidad.
     *
     * <p>Test unificado: se generan entradas que caen en CUALQUIERA de las cinco
     * categorías de rechazo (nombre obligatorio, nombre demasiado largo,
     * descripción obligatoria, descripción demasiado larga, nombre duplicado). Para
     * cada caso se verifica que {@code registerTechnology} termina con un error de
     * dominio ({@link InvalidTechnologyDataException} o
     * {@link TechnologyAlreadyExistsException}) y, sobre todo, que
     * {@code save} NUNCA es invocado.
     *
     * <p>Para la categoría de duplicado se estubiza
     * {@code existsByNameIgnoreCase -> Mono.just(true)} con nombre y descripción
     * válidos; para las categorías de validación no se estubiza {@code save} (por
     * lo que un uso indebido quedaría en evidencia con {@code never()}).
     *
     * <p><b>Validates: Requirements 2.2, 3.4, 4.1, 4.2</b>
     */
    @Property(tries = 100)
    void anyRejectedRequestNeverPersists(@ForAll("rejectionScenarios") RejectionScenario scenario) {

        ITechnologyPersistencePort persistencePort = mock(ITechnologyPersistencePort.class);

        // Solo la categoría de duplicado alcanza la verificación de unicidad; en ese
        // caso el puerto reporta existencia. Las categorías de validación fallan
        // antes de consultar el puerto, por lo que no se estubiza nada más.
        if (scenario.category == RejectionCategory.DUPLICATE_NAME) {
            when(persistencePort.existsByNameIgnoreCase(anyString())).thenReturn(Mono.just(true));
        }

        TechnologyUseCase useCase = new TechnologyUseCase(persistencePort);
        Technology input = new Technology(null, scenario.name, scenario.description);

        StepVerifier.create(useCase.registerTechnology(input))
                .expectErrorSatisfies(error -> Assertions.assertThat(error)
                        .isInstanceOfAny(
                                InvalidTechnologyDataException.class,
                                TechnologyAlreadyExistsException.class))
                .verify();

        // Invariante central de la propiedad: ante cualquier rechazo, jamás se persiste.
        verify(persistencePort, never()).save(any());
    }

    /**
     * Genera escenarios de rechazo distribuidos entre las cinco categorías. Cada
     * escenario está construido para disparar de forma determinista la categoría
     * indicada, considerando el orden de validación del usecase (nombre-requerido,
     * nombre-demasiado-largo, descripción-requerida, descripción-demasiado-larga y,
     * por último, unicidad).
     */
    @Provide
    Arbitrary<RejectionScenario> rejectionScenarios() {
        return Arbitraries.oneOf(
                nameRequiredScenarios(),
                nameTooLongScenarios(),
                descriptionRequiredScenarios(),
                descriptionTooLongScenarios(),
                duplicateNameScenarios());
    }

    /**
     * Categoría 1: nombre que tras {@code trim} queda vacío (null, vacío o solo
     * espacios). La descripción es irrelevante para el resultado porque el nombre
     * falla primero.
     */
    private Arbitrary<RejectionScenario> nameRequiredScenarios() {
        return Combinators.combine(blankOrNull(), anyDescription())
                .as((name, description) ->
                        new RejectionScenario(name, description, RejectionCategory.NAME_REQUIRED, false));
    }

    /**
     * Categoría 2: nombre válido en obligatoriedad pero cuya longitud tras
     * {@code trim} excede 50, con una descripción válida para aislar la regla.
     */
    private Arbitrary<RejectionScenario> nameTooLongScenarios() {
        return Combinators.combine(paddedCore(NAME_MAX_LENGTH + 1, 120), validDescriptionCore())
                .as((name, description) ->
                        new RejectionScenario(name, description, RejectionCategory.NAME_TOO_LONG, false));
    }

    /**
     * Categoría 3: nombre válido y descripción que tras {@code trim} queda vacía
     * (null, vacía o solo espacios).
     */
    private Arbitrary<RejectionScenario> descriptionRequiredScenarios() {
        return Combinators.combine(validNameCore(), blankOrNull())
                .as((name, description) ->
                        new RejectionScenario(name, description, RejectionCategory.DESCRIPTION_REQUIRED, false));
    }

    /**
     * Categoría 4: nombre válido y descripción cuya longitud tras {@code trim}
     * excede 90.
     */
    private Arbitrary<RejectionScenario> descriptionTooLongScenarios() {
        return Combinators.combine(validNameCore(), paddedCore(DESCRIPTION_MAX_LENGTH + 1, 160))
                .as((name, description) ->
                        new RejectionScenario(name, description, RejectionCategory.DESCRIPTION_TOO_LONG, false));
    }

    /**
     * Categoría 5: nombre y descripción válidos (pasan toda la validación
     * sintáctica), pero el puerto reporta que el nombre ya existe, disparando
     * {@link TechnologyAlreadyExistsException} en la etapa de unicidad.
     */
    private Arbitrary<RejectionScenario> duplicateNameScenarios() {
        return Combinators.combine(validNameCore(), validDescriptionCore())
                .as((name, description) ->
                        new RejectionScenario(name, description, RejectionCategory.DUPLICATE_NAME, true));
    }

    /**
     * Genera nombres válidos: 1-50 caracteres imprimibles no-espacio.
     */
    private Arbitrary<String> validNameCore() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withChars('A', 'Z', '0', '9')
                .ofMinLength(1)
                .ofMaxLength(NAME_MAX_LENGTH);
    }

    /**
     * Genera descripciones válidas: 1-90 caracteres imprimibles no-espacio.
     */
    private Arbitrary<String> validDescriptionCore() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withChars('A', 'Z', '0', '9')
                .ofMinLength(1)
                .ofMaxLength(DESCRIPTION_MAX_LENGTH);
    }

    /**
     * Genera cualquier descripción (válida, vacía o nula) usada cuando el resultado
     * no depende de la descripción (por ejemplo, cuando el nombre falla primero).
     */
    private Arbitrary<String> anyDescription() {
        return Arbitraries.oneOf(validDescriptionCore(), blankOrNull());
    }

    /**
     * Genera un núcleo imprimible no-espacio de longitud entre {@code minLength} y
     * {@code maxLength}, rodeado opcionalmente de espacios en blanco de borde (que
     * {@code trim} elimina y por tanto no cuentan hacia la longitud validada).
     */
    private Arbitrary<String> paddedCore(int minLength, int maxLength) {
        Arbitrary<String> core = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withChars('A', 'Z', '0', '9')
                .ofMinLength(minLength)
                .ofMaxLength(maxLength);

        return Combinators.combine(whitespacePadding(), core, whitespacePadding())
                .as((lead, coreValue, trail) -> lead + coreValue + trail);
    }

    /**
     * Genera valores que tras {@code trim} quedan vacíos: {@code null}, cadena
     * vacía o cualquier combinación de solo espacios en blanco.
     */
    private Arbitrary<String> blankOrNull() {
        Arbitrary<String> onlyWhitespace = Arbitraries.of(" ", "\t", "\n", "\r", "\f", "\u000B")
                .list().ofMinSize(0).ofMaxSize(10)
                .map(chars -> String.join("", chars));

        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(""),
                onlyWhitespace);
    }

    /**
     * Genera cadenas compuestas únicamente de caracteres de espacio en blanco
     * (posiblemente vacías) para usarlas como relleno de borde.
     */
    private Arbitrary<String> whitespacePadding() {
        return Arbitraries.of(" ", "\t", "\n", "\r", "\f", "\u000B")
                .list().ofMinSize(0).ofMaxSize(8)
                .map(chars -> String.join("", chars));
    }
}
