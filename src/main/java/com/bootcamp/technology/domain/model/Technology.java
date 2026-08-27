package com.bootcamp.technology.domain.model;

/**
 * Modelo de dominio puro que representa una tecnología.
 *
 * <p>Clase inmutable sin anotaciones de framework: sus valores se fijan en el
 * constructor y solo se exponen mediante getters (no hay setters). El mapeo a
 * persistencia (R2DBC) y a transporte (DTOs) ocurre en los adaptadores, por lo
 * que este modelo permanece libre de acoplamiento a Spring, R2DBC o Jackson.
 */
public final class Technology {

    private final Long id;
    private final String name;
    private final String description;

    public Technology(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
