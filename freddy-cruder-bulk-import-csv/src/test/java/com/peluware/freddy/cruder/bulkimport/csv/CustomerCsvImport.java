package com.peluware.freddy.cruder.bulkimport.csv;

import com.peluware.freddy.cruder.CreateProvider;
import com.peluware.freddy.cruder.bulkimport.ImportConversion;
import com.peluware.freddy.cruder.bulkimport.ImportTemplate;

import java.util.List;

/**
 * Imports customers from a CSV file, whose columns are: 0 name; 1-2 address (street, city),
 * optional as a whole; 3 tags in one cell, separated by {@code ;}; 4-6 up to three phones, one per
 * column; 7-8 and 9-10 up to two contacts, each a group of name and phone.
 */
class CustomerCsvImport extends CsvBulkImportProvider<CustomerInput, CustomerInput> {

    CustomerCsvImport(CreateProvider<CustomerInput, CustomerInput> createProvider) {
        super(createProvider);
    }

    @Override
    protected ImportConversion<CustomerInput> convert(CsvRow row) {
        var input = new CustomerInput();
        var binder = new CsvRowBinder(row);
        binder.required(input::setName, 0, "nombre", CsvCell::textOrNull, "El nombre es obligatorio");

        // A nested object: the same binder, and the path of each field says where it belongs. If
        // none of its columns is filled it is left out; if any is, the rest are required.
        if (!row.cell(1).blank() || !row.cell(2).blank()) {
            var address = new AddressInput();
            binder.required(address::setStreet, 1, "direccion.calle", CsvCell::textOrNull, "La calle es obligatoria");
            binder.required(address::setCity, 2, "direccion.ciudad", CsvCell::textOrNull, "La ciudad es obligatoria");
            input.setAddress(address);
        }

        // A list of simple values in one cell.
        binder.optional(input::setTags, 3, "etiquetas", cell -> cell.textAs(text -> List.of(text.split(";"))));

        // A list of simple values, one per column; an empty column adds nothing.
        for (var index = 0; index < 3; index++) {
            binder.optional(input.phones::add, 4 + index, "telefonos[" + index + "]", CsvCell::textOrNull);
        }

        // A list of objects, as repeated groups of columns; an empty group adds nothing.
        for (var index = 0; index < 2; index++) {
            var first = 7 + index * 2;
            if (row.cell(first).blank() && row.cell(first + 1).blank()) {
                continue;
            }
            var contact = new ContactInput();
            binder.required(contact::setName, first, "contactos[" + index + "].nombre", CsvCell::textOrNull, "El nombre del contacto es obligatorio");
            binder.required(contact::setPhone, first + 1, "contactos[" + index + "].telefono", CsvCell::textOrNull, "El teléfono del contacto es obligatorio");
            input.contacts.add(contact);
        }
        return binder.toConversion(input);
    }

    @Override
    public ImportTemplate template() {
        return new ImportTemplate("clientes.csv", "text/csv", out -> {
        });
    }
}
