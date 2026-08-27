package com.bootcamp.technology.application.config;

import com.bootcamp.technology.domain.api.ITechnologyServicePort;
import com.bootcamp.technology.domain.spi.ITechnologyPersistencePort;
import com.bootcamp.technology.domain.usecase.TechnologyUseCase;
import com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.adapter.TechnologyPersistenceAdapter;
import com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.mapper.TechnologyEntityMapper;
import com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.repository.ITechnologyRepository;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.handler.TechnologyHandler;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.mapper.TechnologyDtoMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * Cableado (wiring) de la arquitectura hexagonal.
 *
 * <p>Concentra en la capa de aplicación la construcción de los beans del dominio
 * y sus adaptadores, de modo que el núcleo ({@link TechnologyUseCase},
 * {@link com.bootcamp.technology.domain.model.Technology} y los puertos) permanece
 * libre de anotaciones de Spring. Los componentes ya gestionados por el framework
 * ({@link ITechnologyRepository}, {@link TechnologyEntityMapper} y
 * {@link TransactionalOperator}) se inyectan aquí para construir el adaptador de
 * persistencia y, con él, el caso de uso.
 *
 * <p>El bean del adaptador driving {@link TechnologyHandler} se registra aquí,
 * inyectando el puerto de entrada {@link ITechnologyServicePort} y el
 * {@link TechnologyDtoMapper}. Así el {@code TechnologyRouter} puede resolver su
 * dependencia del handler sin que este quede anotado con {@code @Component}
 * (evitando beans duplicados) y el dominio permanece libre de anotaciones.
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public ITechnologyPersistencePort technologyPersistencePort(
            ITechnologyRepository repository,
            TechnologyEntityMapper mapper,
            TransactionalOperator transactionalOperator) {
        return new TechnologyPersistenceAdapter(repository, mapper, transactionalOperator);
    }

    @Bean
    public ITechnologyServicePort technologyServicePort(ITechnologyPersistencePort persistencePort) {
        return new TechnologyUseCase(persistencePort);
    }

    @Bean
    public TechnologyHandler technologyHandler(
            ITechnologyServicePort servicePort,
            TechnologyDtoMapper dtoMapper) {
        return new TechnologyHandler(servicePort, dtoMapper);
    }
}
