package com.peluware.freddy.cruder.jpa.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

/** Clave compuesta repartida en varios {@code @Id}: no tiene atributo identificador único. */
@Entity
@IdClass(LegacyCodeId.class)
public class LegacyCode {

    @Id
    public String origin;

    @Id
    public String code;

    public String description;
}
