package com.peluware.freddy.cruder.springframework.web.bulkimport;

import com.peluware.freddy.cruder.CrudContext;
import com.peluware.freddy.cruder.bulkimport.ImportConversion;
import com.peluware.freddy.cruder.bulkimport.ImportProblem;
import com.peluware.freddy.cruder.bulkimport.ImportTemplate;
import com.peluware.freddy.cruder.bulkimport.csv.CsvBulkImportProvider;
import com.peluware.freddy.cruder.bulkimport.csv.CsvRow;

import java.util.List;

/**
 * Imports names from a CSV file, and records each one created together with the option
 * {@code flag} of the request that was current when it was. A name that starts with {@code !} is
 * not convertible, and {@code boom} cannot be created. Its template shows the {@code flag} that is
 * current when the template is written.
 */
class NamesCsvImport extends CsvBulkImportProvider<String, Long> {

    NamesCsvImport(List<String> log) {
        super(name -> {
            if (name.equals("boom")) {
                throw new IllegalStateException("Duplicate name");
            }
            log.add(name + "|" + CrudContext.current().options().getString("flag"));
            return (long) log.size();
        });
    }

    @Override
    protected ImportConversion<String> convert(CsvRow row) {
        var name = row.cell(0).text();
        return name.startsWith("!")
            ? ImportConversion.unconvertible(ImportProblem.of("nombre", "Invalid name"))
            : ImportConversion.converted(name);
    }

    @Override
    public ImportTemplate template() {
        return new ImportTemplate("nombres.csv", "text/csv",
            out -> Templates.text("nombre\n# flag=" + CrudContext.current().options().getString("flag")).writeTo(out));
    }
}
