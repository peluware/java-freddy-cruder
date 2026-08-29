package com.peluware.freddy.cruder.jpa.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/** Asociación a-uno consigo misma, para las rutas de más de dos niveles. */
@Entity
public class Category {

    @Id
    public Long id;

    public String name;

    @ManyToOne(fetch = FetchType.LAZY)
    public Category parent;
}
