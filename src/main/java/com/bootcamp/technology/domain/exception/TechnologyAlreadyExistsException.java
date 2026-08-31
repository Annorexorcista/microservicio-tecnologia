package com.bootcamp.technology.domain.exception;

public class TechnologyAlreadyExistsException extends RuntimeException {

    public TechnologyAlreadyExistsException(String name) {
        super("El nombre '" + name + "' ya está registrado");
    }
}
