package com.peluware.freddy.cruder.bulkimport.csv;

import com.peluware.freddy.cruder.bulkimport.BulkImportRecordException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * How the binder is used when a record is more than flat fields: a nested object, a list of simple
 * values and a list of simple objects, all read from the columns of one record, see
 * {@link CustomerCsvImport}. Nothing here needs more than {@link CsvRowBinder} as it is: the
 * {@code field} of a problem is a path the caller writes, and the accessor is any function of the
 * cell.
 */
class CsvNestedBindingTest {

    private static final String HEADER = "nombre,calle,ciudad,etiquetas,tel1,tel2,tel3,contacto1,tel contacto1,contacto2,tel contacto2\n";
    private static final int COLUMNS = 11;

    private List<CustomerInput> imported;
    private CustomerCsvImport importer;

    @BeforeEach
    void setUp() {
        imported = new ArrayList<>();
        importer = new CustomerCsvImport(input -> {
            imported.add(input);
            return input;
        });
    }

    /**
     * A line of the file: the cells given, then empty ones up to the last column.
     */
    private static InputStream csv(String... cells) {
        var line = new ArrayList<>(List.of(cells));
        while (line.size() < COLUMNS) {
            line.add("");
        }
        return new ByteArrayInputStream((HEADER + String.join(",", line) + "\n").getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void bindsANestedObject() {
        importer.execute(csv("Ana", "Calle 1", "Bogotá"));

        var address = imported.getFirst().address;
        assertEquals("Calle 1", address.street);
        assertEquals("Bogotá", address.city);
    }

    @Test
    void leavesOutANestedObjectWhoseColumnsAreAllEmpty() {
        importer.execute(csv("Ana"));

        assertNull(imported.getFirst().address);
    }

    @Test
    void reportsThePathOfTheMissingFieldOfAHalfFilledNestedObject() {
        var exception = assertThrows(BulkImportRecordException.class, () -> importer.execute(csv("Ana", "Calle 1")));

        assertEquals("direccion.ciudad", exception.problems().getFirst().field());
    }

    @Test
    void bindsAListOfSimpleValuesFromOneCell() {
        importer.execute(csv("Ana", "", "", "vip;moroso;nuevo"));

        assertEquals(List.of("vip", "moroso", "nuevo"), imported.getFirst().tags);
    }

    @Test
    void bindsAListOfSimpleValuesFromSeveralColumnsSkippingTheEmptyOnes() {
        importer.execute(csv("Ana", "", "", "", "5551", "", "5553"));

        assertEquals(List.of("5551", "5553"), imported.getFirst().phones);
    }

    @Test
    void bindsAListOfObjectsFromRepeatedGroupsOfColumnsSkippingTheEmptyOnes() {
        importer.execute(csv("Ana", "", "", "", "", "", "", "Luis", "111"));

        var contacts = imported.getFirst().contacts;
        assertEquals(1, contacts.size());
        assertEquals("Luis", contacts.getFirst().name);
        assertEquals("111", contacts.getFirst().phone);
    }

    @Test
    void reportsThePathOfTheItemOfAListThatIsIncomplete() {
        var exception = assertThrows(BulkImportRecordException.class, () -> importer.execute(csv(
            "Ana", "", "", "", "", "", "", "Luis", "111", "Eva", "")));

        assertEquals("contactos[1].telefono", exception.problems().getFirst().field());
    }
}
