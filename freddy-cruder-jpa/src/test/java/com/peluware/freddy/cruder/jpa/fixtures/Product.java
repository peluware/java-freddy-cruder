package com.peluware.freddy.cruder.jpa.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.util.HashSet;
import java.util.Set;

/** Raíz de las consultas del test: reúne una de cada forma de asociación que {@code findPath} distingue. */
@Entity
public class Product {

    @Id
    public Long id;

    public String name;

    @ManyToOne(fetch = FetchType.LAZY)
    public Category category;

    @OneToMany(mappedBy = "product")
    public Set<Tag> tags = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "legacy_origin", referencedColumnName = "origin"),
        @JoinColumn(name = "legacy_code", referencedColumnName = "code")
    })
    public LegacyCode legacyCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "region_country", referencedColumnName = "country"),
        @JoinColumn(name = "region_area", referencedColumnName = "area")
    })
    public Region region;
}
