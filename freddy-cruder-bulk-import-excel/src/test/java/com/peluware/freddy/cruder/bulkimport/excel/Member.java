package com.peluware.freddy.cruder.bulkimport.excel;

import org.jspecify.annotations.NullUnmarked;

/**
 * A member of a team, the owner.
 */
@NullUnmarked
public class Member {

    Long id;
    Long teamId;
    String name;

    public Member() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
