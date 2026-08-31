package com.bootcamp.technology.domain.spi;

import com.bootcamp.technology.domain.model.Technology;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface ITechnologyPersistencePort {

    Mono<Boolean> existsByNameIgnoreCase(String normalizedName);

    Mono<Technology> save(Technology technology);

    Flux<Technology> findAllByIds(Collection<Long> ids);

    Mono<Void> deleteAllByIds(Collection<Long> ids);
}
