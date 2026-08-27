package com.bootcamp.technology.domain.exception;

/**
 * Códigos de error de negocio del dominio de tecnologías.
 * Cada código asocia una regla de validación con su mensaje de negocio,
 * manteniendo los textos centralizados y libres de acoplamiento HTTP.
 */
public enum DomainErrorCode {

    NAME_REQUIRED("El nombre es obligatorio"),
    NAME_TOO_LONG("El nombre excede la longitud máxima de 50 caracteres"),
    DESCRIPTION_REQUIRED("La descripción es obligatoria"),
    DESCRIPTION_TOO_LONG("La descripción excede la longitud máxima de 90 caracteres");

    private final String message;

    DomainErrorCode(String message) {
        this.message = message;
    }

    /**
     * @return el código de negocio (nombre de la constante del enum).
     */
    public String getCode() {
        return name();
    }

    /**
     * @return el mensaje de negocio asociado al código de error.
     */
    public String getMessage() {
        return message;
    }
}
