package com.peluware.freddy.cruder.springframework.web.bulkimport;

import com.peluware.freddy.cruder.CrudContext;
import com.peluware.freddy.cruder.bulkimport.ImportConversion;
import com.peluware.freddy.cruder.bulkimport.ImportTemplate;
import com.peluware.freddy.cruder.bulkimport.csv.CsvRow;
import com.peluware.freddy.cruder.bulkimport.csv.OwnedCsvBulkImportProvider;

import java.util.List;

/**
 * Imports the names of a team from a CSV file, and records each one created together with the team
 * and the option {@code flag} of the request that was current when it was.
 */
class TeamNamesCsvImport extends OwnedCsvBulkImportProvider<Long, String, Long> {

    TeamNamesCsvImport(List<String> log) {
        super((teamId, name) -> {
            log.add(teamId + ":" + name + "|" + CrudContext.current().options().getString("flag"));
            return (long) log.size();
        });
    }

    @Override
    protected ImportConversion<String> convert(Long ownerId, CsvRow row) {
        return ImportConversion.converted(row.cell(0).text());
    }

    @Override
    public ImportTemplate template(Long ownerId) {
        return new ImportTemplate("nombres-" + ownerId + ".csv", "text/csv", Templates.text("nombre\n"));
    }
}
