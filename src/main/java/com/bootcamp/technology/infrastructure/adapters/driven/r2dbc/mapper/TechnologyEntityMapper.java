package com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.mapper;

import com.bootcamp.technology.domain.model.Technology;
import com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.entity.TechnologyEntity;
import org.springframework.stereotype.Component;

@Component
public class TechnologyEntityMapper {

    public TechnologyEntity toEntity(Technology technology) {
        if (technology == null) {
            return null;
        }
        return new TechnologyEntity(
                technology.getId(),
                technology.getName(),
                technology.getDescription());
    }

    public Technology toDomain(TechnologyEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Technology(
                entity.getId(),
                entity.getName(),
                entity.getDescription());
    }
}
