package com.peluware.freddy.cruder.springframework.web.bulkimport;

import com.peluware.freddy.cruder.bulkimport.OwnedBulkImportProvider;
import com.peluware.freddy.cruder.bulkimport.csv.CsvMetadata;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/teams/{ownerId}/names")
class TeamNamesController implements OwnedBulkImportController<Long, List<String>, CsvMetadata> {

    private final TeamNamesCsvImport service;

    TeamNamesController(TeamNamesCsvImport service) {
        this.service = service;
    }

    @Override
    public OwnedBulkImportProvider<Long, ?, List<String>, CsvMetadata, ?> getBulkImportService() {
        return service;
    }
}
