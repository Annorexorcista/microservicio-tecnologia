package com.bootcamp.technology.infrastructure.adapters.driving.webflux.mapper;

import com.bootcamp.technology.domain.model.Technology;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.dto.TechnologyRequest;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.dto.TechnologyResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper puro (sin I/O ni tipos reactivos) que convierte entre los DTOs de la
 * capa driving (WebFlux) y el modelo de dominio {@link Technology}.
 *
 * <p>Las conversiones son transformaciones en memoria; se invocan dentro del
 * pipeline reactivo del handler (por ejemplo con {@code map}), por lo que este
 * componente no conoce Project Reactor ni detalles de HTTP. El modelo de dominio
 * permanece libre de anotaciones de framework.
 */
@Component
public class TechnologyDtoMapper {

    /**
     * Convierte un DTO de solicitud en modelo de dominio.
     *
     * <p>El {@code id} se fija en {@code null} porque la tecnología aún no ha
     * sido persistida; la base de datos asignará el identificador durante el
     * INSERT. La normalización (trim) y validación se realizan en el dominio.
     *
     * @param request DTO recibido en la solicitud; puede ser {@code null}
     * @return el modelo de dominio equivalente con {@code id} nulo, o {@code null}
     *         si {@code request} es {@code null}
     */
    public Technology toDomain(TechnologyRequest request) {
        if (request == null) {
            return null;
        }
        return new Technology(null, request.name(), request.description());
    }

    /**
     * Convierte un modelo de dominio ya persistido en DTO de respuesta.
     *
     * @param technology modelo de dominio a convertir; puede ser {@code null}
     * @return el DTO de respuesta equivalente, o {@code null} si {@code technology}
     *         es {@code null}
     */
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
