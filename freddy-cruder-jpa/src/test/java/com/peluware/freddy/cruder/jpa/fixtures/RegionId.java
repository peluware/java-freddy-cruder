package com.peluware.freddy.cruder.jpa.fixtures;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RegionId implements Serializable {

    public String country;
    public String area;

    public RegionId() {
    }

    public RegionId(String country, String area) {
        this.country = country;
        this.area = area;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RegionId other
            && Objects.equals(country, other.country)
            && Objects.equals(area, other.area);
    }

    @Override
    public int hashCode() {
        return Objects.hash(country, area);
    }
}
