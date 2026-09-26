package com.peluware.freddy.cruder.bulkimport.excel;

import org.jspecify.annotations.NullUnmarked;

import java.time.LocalDate;

@NullUnmarked
class PersonInput {

    String name;
    Integer age;
    LocalDate birthDate;
    Role role;

    void setName(String name) {
        this.name = name;
    }

    void setAge(Integer age) {
        this.age = age;
    }

    void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    void setRole(Role role) {
        this.role = role;
    }
}
