package com.bootcamp.technology.infrastructure.adapters.driving.webflux.dto;

/**
 * DTO de entrada para el registro de una tecnología.
 *
 * <p>Transporta los datos crudos recibidos en el cuerpo de la solicitud
 * {@code POST /api/v1/technologies}. La normalización (trim) y validación de
 * negocio (obligatoriedad y longitudes) se realizan en el dominio, no aquí.
 *
 * @param name        nombre propuesto para la tecnología
 * @param description descripción propuesta para la tecnología
 */
public record TechnologyRequest(String name, String description) {
}
