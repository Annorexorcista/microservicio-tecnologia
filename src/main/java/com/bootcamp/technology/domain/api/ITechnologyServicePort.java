package com.bootcamp.technology.domain.api;

import com.bootcamp.technology.domain.model.Technology;
import reactor.core.publisher.Mono;

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
}
