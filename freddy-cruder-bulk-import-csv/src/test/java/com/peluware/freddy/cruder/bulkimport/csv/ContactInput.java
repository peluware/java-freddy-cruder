package com.peluware.freddy.cruder.bulkimport.csv;

import org.jspecify.annotations.NullUnmarked;

@NullUnmarked
class ContactInput {

    String name;
    String phone;

    void setName(String name) {
        this.name = name;
    }

    void setPhone(String phone) {
        this.phone = phone;
    }
}
