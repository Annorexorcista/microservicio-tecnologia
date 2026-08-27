package com.bootcamp.technology.domain.spi;

import com.bootcamp.technology.domain.model.Technology;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * Puerto de salida (spi) del dominio para la persistencia de tecnologías.
 *
 * <p>Permite al caso de uso interactuar con el almacenamiento sin conocer la
 * tecnología concreta (R2DBC/MySQL). El adaptador driven
 * {@code TechnologyPersistenceAdapter} lo implementa. Es una abstracción pura,
 * sin anotaciones de framework.
 */
public interface ITechnologyPersistencePort {

    /**
     * Indica si ya existe una tecnología cuyo nombre coincide con el nombre
     * normalizado proporcionado, usando una comparación sin distinción entre
     * mayúsculas y minúsculas.
     *
     * @param normalizedName nombre ya normalizado (con {@code trim} aplicado).
     * @return un {@link Mono} que emite {@code true} si ya existe una tecnología
     *         con ese nombre, o {@code false} en caso contrario.
     */
    Mono<Boolean> existsByNameIgnoreCase(String normalizedName);

    /**
     * Persiste la tecnología proporcionada.
     *
     * @param technology tecnología de dominio a persistir ({@code id == null}).
     * @return un {@link Mono} que emite la tecnología persistida con su
     *         identificador asignado.
     */
    Mono<Technology> save(Technology technology);

    /**
     * Recupera las tecnologías cuyos identificadores están contenidos en la
     * colección proporcionada. Solo emite las que existen.
     *
     * @param ids identificadores de tecnología a consultar.
     * @return un {@link Flux} con las tecnologías existentes (0..N elementos).
     */
    Flux<Technology> findAllByIds(Collection<Long> ids);
}
