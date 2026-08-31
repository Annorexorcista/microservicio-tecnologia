package com.bootcamp.technology.domain.api;

import com.bootcamp.technology.domain.model.Technology;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface ITechnologyServicePort {

    Mono<Technology> registerTechnology(Technology technology);

    Flux<Technology> findTechnologiesByIds(Collection<Long> ids);

    Mono<Void> deleteTechnologiesByIds(Collection<Long> ids);
}
