package com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.repository;

import com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.entity.TechnologyEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Repositorio reactivo R2DBC para la entidad {@link TechnologyEntity}.
 *
 * <p>Extiende {@link ReactiveCrudRepository} para heredar las operaciones CRUD
 * no bloqueantes (por ejemplo, {@code save} que retorna {@code Mono<TechnologyEntity>}).
 * La derived query {@code existsByNameIgnoreCase} genera una consulta que
 * compara el nombre sin distinción entre mayúsculas y minúsculas.
 */
public interface ITechnologyRepository extends ReactiveCrudRepository<TechnologyEntity, Long> {

    /**
     * Indica si existe una tecnología cuyo nombre coincide con el proporcionado,
     * ignorando mayúsculas y minúsculas.
     *
     * @param name nombre a comparar.
     * @return un {@link Mono} que emite {@code true} si existe, {@code false} en caso contrario.
     */
    Mono<Boolean> existsByNameIgnoreCase(String name);
}
