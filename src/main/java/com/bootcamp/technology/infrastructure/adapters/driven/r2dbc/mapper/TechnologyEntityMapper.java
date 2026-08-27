package com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.mapper;

import com.bootcamp.technology.domain.model.Technology;
import com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.entity.TechnologyEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper puro (sin I/O ni tipos reactivos) que convierte entre el modelo de
 * dominio {@link Technology} y la entidad de persistencia {@link TechnologyEntity}.
 *
 * <p>Las conversiones son transformaciones en memoria; se invocan dentro del
 * pipeline reactivo del adaptador (por ejemplo envueltas en {@code Mono.fromCallable}),
 * por lo que este componente no conoce Project Reactor ni R2DBC.
 */
@Component
public class TechnologyEntityMapper {

    /**
     * Convierte un modelo de dominio en entidad de persistencia.
     *
     * <p>Cuando el {@code id} del dominio es {@code null}, el {@code id} de la
     * entidad también queda en {@code null}, de modo que Spring Data R2DBC trate
     * la fila como nueva ({@code isNew}) y ejecute un INSERT.
     *
     * @param technology modelo de dominio a convertir; puede ser {@code null}
     * @return la entidad equivalente, o {@code null} si {@code technology} es {@code null}
     */
    public TechnologyEntity toEntity(Technology technology) {
        if (technology == null) {
            return null;
        }
        return new TechnologyEntity(
                technology.getId(),
                technology.getName(),
                technology.getDescription());
    }

    /**
     * Convierte una entidad de persistencia en modelo de dominio.
     *
     * @param entity entidad a convertir; puede ser {@code null}
     * @return el modelo de dominio equivalente, o {@code null} si {@code entity} es {@code null}
     */
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
