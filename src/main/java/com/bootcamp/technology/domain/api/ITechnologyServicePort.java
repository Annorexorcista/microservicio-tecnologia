package com.bootcamp.technology.domain.api;

import com.bootcamp.technology.domain.model.Technology;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * Puerto de entrada (api) del dominio para el registro de tecnologías.
 *
 * <p>Define el contrato que la capa driving (WebFlux) consume para orquestar el
 * caso de uso de registro. Es una abstracción pura: no conoce detalles de HTTP,
 * Spring ni persistencia. El caso de uso {@code TechnologyUseCase} lo implementa.
 */
public interface ITechnologyServicePort {

    /**
     * Registra una tecnología aplicando las reglas de negocio (normalización,
     * validaciones de obligatoriedad/longitud y unicidad del nombre).
     *
     * @param technology tecnología de dominio a registrar ({@code id == null}).
     * @return un {@link Mono} que emite la tecnología persistida con su identificador
     *         asignado, o un error de dominio si la validación o la unicidad fallan.
     */
    Mono<Technology> registerTechnology(Technology technology);

    /**
     * Recupera las tecnologías cuyos identificadores están contenidos en la
     * colección proporcionada. Solo se emiten las tecnologías que existen; los
     * identificadores inexistentes simplemente no producen ningún elemento.
     *
     * <p>Pensado para consumo entre microservicios (por ejemplo, el de capacidad,
     * que valida existencia y enriquece con id/nombre a partir de una lista de ids).
     *
     * @param ids identificadores de tecnología a consultar.
     * @return un {@link Flux} con las tecnologías existentes (0..N elementos).
     */
    Flux<Technology> findTechnologiesByIds(Collection<Long> ids);
}
