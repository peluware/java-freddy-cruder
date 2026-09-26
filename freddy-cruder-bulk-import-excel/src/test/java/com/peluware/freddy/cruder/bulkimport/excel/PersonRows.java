package com.peluware.freddy.cruder.bulkimport.excel;

import com.peluware.freddy.cruder.bulkimport.ImportConversion;

/**
 * The sheet of people and how a row of it becomes a {@link PersonInput}, the same whatever way the
 * file is read.
 */
final class PersonRows {

    static final String SHEET = "Personas";

    private PersonRows() {
    }

    static ImportConversion<PersonInput> convert(ExcelRow row) {
        var input = new PersonInput();
        var binder = new ExcelRowBinder(row);
        binder.required(input::setName, 0, "nombre", ExcelCell::textOrNull, "El nombre es obligatorio");
        binder.required(input::setAge, 1, "edad", ExcelCell::integer, "La edad es obligatoria");
        binder.optional(input::setBirthDate, 2, "nacimiento", ExcelCell::date);
        binder.required(input::setRole, 3, "rol", cell -> cell.enumeration(Role.class), "El rol es obligatorio");
        return binder.toConversion(input);
    }
}
