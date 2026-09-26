package com.peluware.freddy.cruder.bulkimport;

import org.jspecify.annotations.NullUnmarked;

@NullUnmarked
class AccountInput {

    String code;
    Integer level;

    void setCode(String code) {
        this.code = code;
    }

    void setLevel(Integer level) {
        this.level = level;
    }
}
