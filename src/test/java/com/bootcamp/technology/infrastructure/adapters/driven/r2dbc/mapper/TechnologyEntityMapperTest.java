package com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.mapper;

import com.bootcamp.technology.domain.model.Technology;
import com.bootcamp.technology.infrastructure.adapters.driven.r2dbc.entity.TechnologyEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios del {@link TechnologyEntityMapper}. Verifican que las
 * conversiones dominio &lt;-&gt; entidad preserven los datos y que un {@code id}
 * de dominio {@code null} produzca una entidad con {@code id} {@code null}
 * (para que R2DBC la trate como nueva -&gt; INSERT).
 */
class TechnologyEntityMapperTest {

    private TechnologyEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TechnologyEntityMapper();
    }

    @Test
    @DisplayName("toEntity mapea todos los campos del dominio a la entidad")
    void toEntityMapsAllFields() {
        Technology technology = new Technology(7L, "Java", "Lenguaje de programación");

        TechnologyEntity entity = mapper.toEntity(technology);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(7L);
        assertThat(entity.getName()).isEqualTo("Java");
        assertThat(entity.getDescription()).isEqualTo("Lenguaje de programación");
    }

    @Test
    @DisplayName("toEntity con id null produce entidad con id null (nuevo -> INSERT)")
    void toEntityWithNullIdKeepsIdNull() {
        Technology technology = new Technology(null, "Python", "Lenguaje interpretado");

        TechnologyEntity entity = mapper.toEntity(technology);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isNull();
        assertThat(entity.getName()).isEqualTo("Python");
        assertThat(entity.getDescription()).isEqualTo("Lenguaje interpretado");
    }

    @Test
    @DisplayName("toEntity con null retorna null")
    void toEntityWithNullReturnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("toDomain mapea todos los campos de la entidad al dominio")
    void toDomainMapsAllFields() {
        TechnologyEntity entity = new TechnologyEntity(3L, "Go", "Lenguaje compilado");

        Technology technology = mapper.toDomain(entity);

        assertThat(technology).isNotNull();
        assertThat(technology.getId()).isEqualTo(3L);
        assertThat(technology.getName()).isEqualTo("Go");
        assertThat(technology.getDescription()).isEqualTo("Lenguaje compilado");
    }

    @Test
    @DisplayName("toDomain con null retorna null")
    void toDomainWithNullReturnsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    @DisplayName("round-trip dominio -> entidad -> dominio preserva los datos")
    void roundTripPreservesData() {
        Technology original = new Technology(11L, "Rust", "Seguridad de memoria");

        Technology result = mapper.toDomain(mapper.toEntity(original));

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(original.getId());
        assertThat(result.getName()).isEqualTo(original.getName());
        assertThat(result.getDescription()).isEqualTo(original.getDescription());
    }
}
