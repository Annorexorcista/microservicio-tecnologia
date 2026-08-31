package com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.repository;

import com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.entity.TechnologyEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ITechnologyRepository extends ReactiveCrudRepository<TechnologyEntity, Long> {

    Mono<Boolean> existsByNameIgnoreCase(String name);
}
