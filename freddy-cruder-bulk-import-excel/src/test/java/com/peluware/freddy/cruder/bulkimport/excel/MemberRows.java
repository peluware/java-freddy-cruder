package com.peluware.freddy.cruder.bulkimport.excel;

import com.peluware.freddy.cruder.bulkimport.ImportConversion;

/**
 * The sheet of members and how a row of it becomes a {@link MemberInput}. The name of each member
 * is followed by {@code @} and the team, so that a test can see that the owner reached the
 * conversion.
 */
final class MemberRows {

    static final String SHEET = "Miembros";

    private MemberRows() {
    }

    static ImportConversion<MemberInput> convert(Long ownerId, ExcelRow row) {
        var input = new MemberInput();
        var binder = new ExcelRowBinder(row);
        binder.required(name -> input.setName(name + "@" + ownerId), 0, "nombre", ExcelCell::textOrNull, "El nombre es obligatorio");
        return binder.toConversion(input);
    }
}
