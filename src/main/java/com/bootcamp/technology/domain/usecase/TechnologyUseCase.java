package com.bootcamp.technology.domain.usecase;

import com.bootcamp.technology.domain.api.ITechnologyServicePort;
import com.bootcamp.technology.domain.exception.DomainErrorCode;
import com.bootcamp.technology.domain.exception.InvalidTechnologyDataException;
import com.bootcamp.technology.domain.exception.TechnologyAlreadyExistsException;
import com.bootcamp.technology.domain.model.Technology;
import com.bootcamp.technology.domain.spi.ITechnologyPersistencePort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public class TechnologyUseCase implements ITechnologyServicePort {

    private static final int NAME_MAX_LENGTH = 50;
    private static final int DESCRIPTION_MAX_LENGTH = 90;

    private final ITechnologyPersistencePort persistencePort;

    public TechnologyUseCase(ITechnologyPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    @Override
    public Mono<Technology> registerTechnology(Technology technology) {
        return validate(technology)
                .flatMap(this::ensureNameIsUnique)
                .flatMap(persistencePort::save);
    }

    @Override
    public Flux<Technology> findTechnologiesByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Flux.empty();
        }
        return persistencePort.findAllByIds(ids);
    }

    @Override
    public Mono<Void> deleteTechnologiesByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Mono.empty();
        }
        return persistencePort.deleteAllByIds(ids);
    }

    private Mono<Technology> validate(Technology technology) {
        return Mono.defer(() -> {
            if (technology == null) {
                return Mono.error(
                        new InvalidTechnologyDataException(
                                DomainErrorCode.TECHNOLOGY_REQUIRED));
            }

            String name = technology.getName() == null
                    ? null
                    : technology.getName().trim();

            String description = technology.getDescription() == null
                    ? null
                    : technology.getDescription().trim();

            if (name == null || name.isEmpty()) {
                return Mono.error(
                        new InvalidTechnologyDataException(
                                DomainErrorCode.NAME_REQUIRED));
            }

            if (name.length() > NAME_MAX_LENGTH) {
                return Mono.error(
                        new InvalidTechnologyDataException(
                                DomainErrorCode.NAME_TOO_LONG));
            }

            if (description == null || description.isEmpty()) {
                return Mono.error(
                        new InvalidTechnologyDataException(
                                DomainErrorCode.DESCRIPTION_REQUIRED));
            }

            if (description.length() > DESCRIPTION_MAX_LENGTH) {
                return Mono.error(
                        new InvalidTechnologyDataException(
                                DomainErrorCode.DESCRIPTION_TOO_LONG));
            }

            Technology normalizedTechnology = new Technology(null, name, description);

            return Mono.just(normalizedTechnology);
        });
    }

    private Mono<Technology> ensureNameIsUnique(Technology technology) {
        return persistencePort.existsByNameIgnoreCase(technology.getName())
                .flatMap(exists -> Boolean.TRUE.equals(exists)
                        ? Mono.<Technology>error(new TechnologyAlreadyExistsException(technology.getName()))
                        : Mono.just(technology));
    }
}
