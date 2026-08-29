package com.peluware.freddy.cruder.jpa.fixtures;

import java.io.Serializable;
import java.util.Objects;

public class LegacyCodeId implements Serializable {

    public String origin;
    public String code;

    public LegacyCodeId() {
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LegacyCodeId other
            && Objects.equals(origin, other.origin)
            && Objects.equals(code, other.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, code);
    }
}
