package com.bootcamp.technology.domain.exception;

public class InvalidTechnologyDataException extends RuntimeException {

    private final DomainErrorCode code;

    public InvalidTechnologyDataException(DomainErrorCode code) {
        super(code.getMessage());
        this.code = code;
    }

    public DomainErrorCode getCode() {
        return code;
    }
}
