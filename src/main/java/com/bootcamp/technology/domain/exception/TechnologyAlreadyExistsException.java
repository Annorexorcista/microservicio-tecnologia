package com.bootcamp.technology.domain.exception;

/**
 * Excepción de dominio lanzada cuando se intenta registrar una tecnología cuyo
 * nombre ya existe (comparación case-insensitive y con trim).
 * Es una excepción pura, sin dependencias de HTTP; el handler global la traduce
 * al código de estado correspondiente (409 Conflict).
 */
public class TechnologyAlreadyExistsException extends RuntimeException {

    public TechnologyAlreadyExistsException(String name) {
        super("El nombre '" + name + "' ya está registrado");
    }
}
