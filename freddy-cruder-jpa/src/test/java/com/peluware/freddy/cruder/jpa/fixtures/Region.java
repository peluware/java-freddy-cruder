package com.peluware.freddy.cruder.jpa.fixtures;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

/** Clave compuesta en un solo atributo: {@code hasSingleIdAttribute()} responde que sí. */
@Entity
public class Region {

    @EmbeddedId
    public RegionId id;

    public String name;
}
