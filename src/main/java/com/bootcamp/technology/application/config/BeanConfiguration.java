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
