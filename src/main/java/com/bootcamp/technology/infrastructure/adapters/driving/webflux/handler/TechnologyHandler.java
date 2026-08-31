package com.bootcamp.technology.infrastructure.adapters.driving.webflux.handler;

import com.bootcamp.technology.domain.api.ITechnologyServicePort;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.dto.TechnologyRequest;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.exception.InvalidIdsQueryException;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.exception.RequestErrorCode;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.mapper.TechnologyDtoMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

public class TechnologyHandler {

    private final ITechnologyServicePort servicePort;
    private final TechnologyDtoMapper dtoMapper;

    public TechnologyHandler(ITechnologyServicePort servicePort, TechnologyDtoMapper dtoMapper) {
        this.servicePort = servicePort;
        this.dtoMapper = dtoMapper;
    }

    public Mono<ServerResponse> register(ServerRequest request) {
        return request.bodyToMono(TechnologyRequest.class)
                .map(dtoMapper::toDomain)
                .flatMap(servicePort::registerTechnology)
                .map(dtoMapper::toResponse)
                .flatMap(response -> ServerResponse
                        .status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    public Mono<ServerResponse> findByIds(ServerRequest request) {
        return Mono.fromCallable(() -> parseRequiredIds(request))
                .flatMapMany(servicePort::findTechnologiesByIds)
                .map(dtoMapper::toResponse)
                .collectList()
                .flatMap(responses -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(responses));
    }

    public Mono<ServerResponse> deleteByIds(ServerRequest request) {
        return Mono.fromCallable(() -> parseRequiredIds(request))
                .flatMap(servicePort::deleteTechnologiesByIds)
                .then(ServerResponse.noContent().build());
    }

    private List<Long> parseRequiredIds(ServerRequest request) {
        return request.queryParam("ids")
                .map(this::parseIds)
                .orElseThrow(() -> new InvalidIdsQueryException(
                        RequestErrorCode.IDS_REQUIRED));
    }

    private List<Long> parseIds(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidIdsQueryException(
                    RequestErrorCode.IDS_REQUIRED);
        }

        return Arrays.stream(raw.split(",", -1))
                .map(String::trim)
                .map(this::parsePositiveId)
                .distinct()
                .toList();
    }

    private Long parsePositiveId(String segment) {
        if (segment.isEmpty()) {
            throw new InvalidIdsQueryException(
                    RequestErrorCode.ID_EMPTY);
        }

        if (!segment.matches("[0-9]+")) {
            throw new InvalidIdsQueryException(
                    RequestErrorCode.ID_NOT_NUMERIC,
                    segment);
        }

        try {
            long id = Long.parseLong(segment);

            if (id <= 0) {
                throw new InvalidIdsQueryException(
                        RequestErrorCode.ID_NOT_POSITIVE,
                        segment);
            }

            return id;
        } catch (NumberFormatException exception) {
            throw new InvalidIdsQueryException(
                    RequestErrorCode.ID_OUT_OF_RANGE,
                    exception,
                    segment);
        }
    }
}
