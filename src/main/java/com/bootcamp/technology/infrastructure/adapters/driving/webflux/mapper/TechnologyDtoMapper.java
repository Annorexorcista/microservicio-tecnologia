package com.bootcamp.technology.infrastructure.adapters.driving.webflux.mapper;

import com.bootcamp.technology.domain.model.Technology;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.dto.TechnologyRequest;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.dto.TechnologyResponse;
import org.springframework.stereotype.Component;

@Component
public class TechnologyDtoMapper {

    public Technology toDomain(TechnologyRequest request) {
        if (request == null) {
            return null;
        }
        return new Technology(null, request.name(), request.description());
    }

    public TechnologyResponse toResponse(Technology technology) {
        if (technology == null) {
            return null;
        }
        return new TechnologyResponse(
                technology.getId(),
                technology.getName(),
                technology.getDescription());
    }
}
