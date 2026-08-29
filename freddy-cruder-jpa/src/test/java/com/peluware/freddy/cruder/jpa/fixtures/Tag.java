package com.peluware.freddy.cruder.jpa.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Tag {

    @Id
    public Long id;

    public String label;

    @ManyToOne(fetch = FetchType.LAZY)
    public Product product;
}
