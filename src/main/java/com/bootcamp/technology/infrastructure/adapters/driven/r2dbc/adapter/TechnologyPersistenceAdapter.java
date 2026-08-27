package com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.adapter;

import com.bootcamp.technology.domain.exception.TechnologyAlreadyExistsException;
import com.bootcamp.technology.domain.model.Technology;
import com.bootcamp.technology.domain.spi.ITechnologyPersistencePort;
import com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.mapper.TechnologyEntityMapper;
import com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.repository.ITechnologyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

/**
 * Adaptador driven que implementa el puerto de salida {@link ITechnologyPersistencePort}
 * usando Spring Data R2DBC.
 *
 * <p>Traduce entre el modelo de dominio y la entidad de persistencia mediante el
 * {@link TechnologyEntityMapper}, delega las operaciones no bloqueantes en el
 * {@link ITechnologyRepository} y envuelve la escritura en una transacción reactiva
 * mediante el {@link TransactionalOperator}.
 *
 * <p>No se anota con {@code @Component}: el wiring hexagonal se realiza en
 * {@code BeanConfiguration} para mantener el dominio y el adaptador libres de
 * acoplamiento a la configuración de Spring. Es un flujo totalmente reactivo,
 * sin llamadas bloqueantes ({@code .block()}).
 */
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

    /**
     * {@inheritDoc}
     *
     * <p>Delega directamente en la derived query del repositorio, que compara el
     * nombre sin distinción entre mayúsculas y minúsculas.
     */
    @Override
    public Mono<Boolean> existsByNameIgnoreCase(String normalizedName) {
        return repository.existsByNameIgnoreCase(normalizedName);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Compone un pipeline reactivo que mapea el dominio a entidad, la persiste y
     * mapea de vuelta a dominio, todo dentro de una transacción reactiva. Ante una
     * {@link DataIntegrityViolationException} (por ejemplo, una condición de carrera
     * sobre la restricción UNIQUE del nombre) se reasigna a
     * {@link TechnologyAlreadyExistsException} para reforzar la respuesta 409.
     */
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
