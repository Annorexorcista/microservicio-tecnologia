package com.bootcamp.technology.infrastructure.adapters.driving.webflux.dto;

import java.time.Instant;

/**
 * DTO de salida para representar errores de forma uniforme.
 *
 * <p>Lo produce el manejador global de errores al traducir las excepciones de
 * dominio y de entrada a respuestas HTTP (400, 409, 500), manteniendo un
 * contrato de error consistente para los consumidores de la API.
 *
 * @param status    código de estado HTTP asociado al error
 * @param code       código de negocio o categoría del error
 * @param message   mensaje descriptivo del error
 * @param timestamp instante en que se generó la respuesta de error
 */
public record ErrorResponse(int status, String code, String message, Instant timestamp) {
}
