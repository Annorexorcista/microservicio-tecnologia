package com.bootcamp.technology.infrastructure.adapters.driving.webflux.dto;

/**
 * DTO de salida que representa una tecnología ya registrada.
 *
 * <p>Se retorna en la respuesta {@code 201 Created} del endpoint de registro,
 * exponiendo el identificador asignado por la base de datos junto con el nombre
 * y la descripción normalizados.
 *
 * @param id          identificador generado por la persistencia
 * @param name        nombre normalizado de la tecnología
 * @param description descripción normalizada de la tecnología
 */
public record TechnologyResponse(Long id, String name, String description) {
}
