package com.peluware.freddy.cruder.bulkimport.csv;

import org.jspecify.annotations.NullUnmarked;

@NullUnmarked
class AddressInput {

    String street;
    String city;

    void setStreet(String street) {
        this.street = street;
    }

    void setCity(String city) {
        this.city = city;
    }
}
