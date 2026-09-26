package com.peluware.freddy.cruder.bulkimport.excel;

import com.peluware.freddy.cruder.bulkimport.ImportConversion;
import com.peluware.freddy.cruder.bulkimport.ImportTemplate;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Imports people from files that need a password, which it does by overriding how they are opened.
 */
class PasswordPersonImport extends ExcelBulkImportProvider<PersonInput, Long> {

    private final String password;

    PasswordPersonImport(PersonProvider people, String password) {
        super(people);
        this.password = password;
    }

    @Override
    protected Workbook openWorkbook(InputStream in) {
        try {
            return WorkbookFactory.create(in, password);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected String sheetName() {
        return PersonRows.SHEET;
    }

    @Override
    protected ImportConversion<PersonInput> convert(ExcelRow row) {
        return PersonRows.convert(row);
    }

    @Override
    public ImportTemplate template() {
        return new ImportTemplate("personas.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out -> {
        });
    }
}
