package com.peluware.freddy.cruder.bulkimport.excel;

import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.List;

/**
 * Builds the {@code .xlsx} files that the tests read.
 */
final class Workbooks {

    /**
     * A formula, written without the result that Excel would save with it.
     */
    record Formula(String expression) {
    }

    private Workbooks() {
    }

    /**
     * A workbook with the given sheet: a header row, then one row per array of values. A
     * {@code String} is written as text, an {@code Integer} as a number, a {@code LocalDate} as a
     * date and a {@link Formula} as a formula; a {@code null} leaves its cell empty.
     */
    static InputStream workbook(String sheetName, @Nullable Object[]... rows) {
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));
            var sheet = workbook.createSheet(sheetName);
            var header = sheet.createRow(0);
            var headers = List.of("nombre", "edad", "nacimiento", "rol");
            for (var column = 0; column < headers.size(); column++) {
                header.createCell(column).setCellValue(headers.get(column));
            }
            for (var index = 0; index < rows.length; index++) {
                var row = sheet.createRow(index + 1);
                for (var column = 0; column < rows[index].length; column++) {
                    var value = rows[index][column];
                    if (value == null) {
                        continue;
                    }
                    var cell = row.createCell(column);
                    switch (value) {
                        case String text -> cell.setCellValue(text);
                        case Integer number -> cell.setCellValue(number);
                        case LocalDate date -> {
                            cell.setCellValue(date);
                            cell.setCellStyle(dateStyle);
                        }
                        case Formula formula -> cell.setCellFormula(formula.expression());
                        default -> throw new IllegalArgumentException("Unsupported value: " + value);
                    }
                }
            }
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A workbook with the given sheet holding one name per row.
     */
    static InputStream names(String sheetName, String... names) {
        var rows = new @Nullable Object[names.length][];
        for (var index = 0; index < names.length; index++) {
            rows[index] = new @Nullable Object[]{names[index]};
        }
        return workbook(sheetName, rows);
    }

    /**
     * The same workbook, protected with a password.
     */
    static InputStream encrypted(String password, InputStream workbook) {
        try (var plain = new XSSFWorkbook(workbook); var fileSystem = new POIFSFileSystem(); var out = new ByteArrayOutputStream()) {
            var encryptor = new EncryptionInfo(EncryptionMode.agile).getEncryptor();
            encryptor.confirmPassword(password);
            try (var encrypted = encryptor.getDataStream(fileSystem)) {
                plain.write(encrypted);
            }
            fileSystem.writeFilesystem(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * A row of the sheet of people.
     */
    static @Nullable Object[] person(@Nullable String name, @Nullable Integer age, @Nullable LocalDate birthDate, @Nullable String role) {
        return new @Nullable Object[]{name, age, birthDate, role};
    }
}
