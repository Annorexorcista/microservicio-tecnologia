package com.bootcamp.technology.domain.usecase;

import com.bootcamp.technology.domain.api.ITechnologyServicePort;
import com.bootcamp.technology.domain.exception.DomainErrorCode;
import com.bootcamp.technology.domain.exception.InvalidTechnologyDataException;
import com.bootcamp.technology.domain.exception.TechnologyAlreadyExistsException;
import com.bootcamp.technology.domain.model.Technology;
import com.bootcamp.technology.domain.spi.ITechnologyPersistencePort;
import reactor.core.publisher.Mono;

/**
 * Caso de uso de dominio para el registro de tecnologías.
 *
 * <p>Clase de dominio pura (sin anotaciones de framework) que implementa el
 * puerto de entrada {@link ITechnologyServicePort} y concentra todas las reglas
 * de negocio: normalización ({@code trim}), validaciones sintácticas de
 * obligatoriedad y longitud, y unicidad del nombre. Depende del puerto de salida
 * {@link ITechnologyPersistencePort} inyectado por constructor.
 *
 * <p>El pipeline es completamente reactivo y no bloqueante: nunca se invoca
 * {@code .block()}. Cualquier fallo se emite como {@link Mono#error} y
 * cortocircuita el resto del flujo, garantizando que no se persista nada cuando
 * una validación o la unicidad fallan.
 */
public class TechnologyUseCase implements ITechnologyServicePort {

    private static final int NAME_MAX_LENGTH = 50;
    private static final int DESCRIPTION_MAX_LENGTH = 90;

    private final ITechnologyPersistencePort persistencePort;

    public TechnologyUseCase(ITechnologyPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    /**
     * Registra una tecnología componiendo el pipeline reactivo de validación,
     * verificación de unicidad y persistencia.
     *
     * @param technology tecnología de dominio a registrar ({@code id == null}).
     * @return un {@link Mono} que emite la tecnología persistida con su
     *         identificador asignado, o un error de dominio si la validación o
     *         la unicidad fallan (en cuyo caso no se persiste nada).
     */
    @Override
    public Mono<Technology> registerTechnology(Technology technology) {
        return validate(technology)
                .flatMap(this::ensureNameIsUnique)
                .flatMap(persistencePort::save);
    }

    /**
     * Normaliza (aplica {@code trim}) y valida sintácticamente la tecnología.
     *
     * <p>Reglas: nombre y descripción son obligatorios (null/vacío/solo espacios
     * se rechazan); el nombre no puede exceder 50 caracteres ni la descripción 90,
     * medidos tras aplicar {@code trim}. Si todo es válido emite un
     * {@link Technology} normalizado con {@code id == null}.
     *
     * @param technology tecnología de entrada sin normalizar.
     * @return un {@link Mono} con la tecnología normalizada, o
     *         {@link Mono#error} con {@link InvalidTechnologyDataException}.
     */
    private Mono<Technology> validate(Technology technology) {
        String name = technology.getName() == null ? null : technology.getName().trim();
        String description = technology.getDescription() == null ? null : technology.getDescription().trim();

        if (name == null || name.isEmpty()) {
            return Mono.error(new InvalidTechnologyDataException(DomainErrorCode.NAME_REQUIRED));
        }
        if (name.length() > NAME_MAX_LENGTH) {
            return Mono.error(new InvalidTechnologyDataException(DomainErrorCode.NAME_TOO_LONG));
        }
        if (description == null || description.isEmpty()) {
            return Mono.error(new InvalidTechnologyDataException(DomainErrorCode.DESCRIPTION_REQUIRED));
        }
        if (description.length() > DESCRIPTION_MAX_LENGTH) {
            return Mono.error(new InvalidTechnologyDataException(DomainErrorCode.DESCRIPTION_TOO_LONG));
        }
        return Mono.just(new Technology(null, name, description));
    }

    /**
     * Verifica que el nombre normalizado no exista ya (comparación sin distinción
     * de mayúsculas/minúsculas realizada por el puerto de persistencia).
     *
     * <p>Si el nombre existe emite {@link Mono#error} con
     * {@link TechnologyAlreadyExistsException} sin persistir; en caso contrario
     * continúa emitiendo la tecnología.
     *
     * @param technology tecnología ya normalizada por {@link #validate}.
     * @return un {@link Mono} con la tecnología si el nombre es único, o
     *         {@link Mono#error} si ya existe.
     */
    private Mono<Technology> ensureNameIsUnique(Technology technology) {
        return persistencePort.existsByNameIgnoreCase(technology.getName())
                .flatMap(exists -> Boolean.TRUE.equals(exists)
                        ? Mono.<Technology>error(new TechnologyAlreadyExistsException(technology.getName()))
                        : Mono.just(technology));
    }
}
