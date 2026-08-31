package com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.adapter;

import com.bootcamp.technology.domain.exception.TechnologyAlreadyExistsException;
import com.bootcamp.technology.domain.model.Technology;
import com.bootcamp.technology.domain.spi.ITechnologyPersistencePort;
import com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.mapper.TechnologyEntityMapper;
import com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.repository.ITechnologyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public class TechnologyPersistenceAdapter implements ITechnologyPersistencePort {

    private final ITechnologyRepository repository;
    private final TechnologyEntityMapper mapper;
    private final TransactionalOperator transactionalOperator;

    public TechnologyPersistenceAdapter(ITechnologyRepository repository,
                                        TechnologyEntityMapper mapper,
                                        TransactionalOperator transactionalOperator) {
        this.repository = repository;
        this.mapper = mapper;
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public Mono<Boolean> existsByNameIgnoreCase(String normalizedName) {
        return repository.existsByNameIgnoreCase(normalizedName);
    }

    @Override
    public Flux<Technology> findAllByIds(Collection<Long> ids) {
        return repository.findAllById(ids)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Void> deleteAllByIds(Collection<Long> ids) {
        return repository.deleteAllById(ids)
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<Technology> save(Technology technology) {
        return Mono.fromCallable(() -> mapper.toEntity(technology))
                .flatMap(repository::save)
                .map(mapper::toDomain)
                .as(transactionalOperator::transactional)
                .onErrorMap(DataIntegrityViolationException.class,
                        ex -> new TechnologyAlreadyExistsException(technology.getName()));
    }
}
