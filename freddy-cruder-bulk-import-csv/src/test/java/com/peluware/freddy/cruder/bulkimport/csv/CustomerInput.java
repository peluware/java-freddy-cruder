package com.peluware.freddy.cruder.bulkimport.csv;

import org.jspecify.annotations.NullUnmarked;

import java.util.ArrayList;
import java.util.List;

/**
 * More than flat fields: a nested object, a list of simple values read from one cell, another read
 * from several columns, and a list of objects.
 */
@NullUnmarked
class CustomerInput {

    String name;
    AddressInput address;
    List<String> tags = new ArrayList<>();
    List<String> phones = new ArrayList<>();
    List<ContactInput> contacts = new ArrayList<>();

    void setName(String name) {
        this.name = name;
    }

    void setAddress(AddressInput address) {
        this.address = address;
    }

    void setTags(List<String> tags) {
        this.tags = tags;
    }
}
