package com.bootcamp.technology.domain.exception;

/**
 * Excepción de dominio lanzada cuando los datos de una tecnología no superan
 * las validaciones sintácticas de negocio (obligatoriedad y longitudes).
 * Es una excepción pura, sin dependencias de HTTP; porta un {@link DomainErrorCode}
 * que el handler global traduce al código de estado correspondiente.
 */
public class InvalidTechnologyDataException extends RuntimeException {

    private final DomainErrorCode code;

    public InvalidTechnologyDataException(DomainErrorCode code) {
        super(code.getMessage());
        this.code = code;
    }

    /**
     * @return el código de error de negocio que originó esta excepción.
     */
    public DomainErrorCode getCode() {
        return code;
    }
}
