package com.peluware.freddy.cruder.springframework.web.export;

import org.jspecify.annotations.NullUnmarked;

@NullUnmarked
public class TeamPerson {

    Long id;
    Long teamId;
    String name;

    public TeamPerson() {
    }

    Long getId() {
        return id;
    }

    void setId(Long id) {
        this.id = id;
    }
}
