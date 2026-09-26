package com.peluware.freddy.cruder.springframework.web.bulkimport;

import com.peluware.freddy.cruder.bulkimport.BulkImportProvider;
import com.peluware.freddy.cruder.bulkimport.csv.CsvMetadata;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/names")
class NamesController implements BulkImportController<List<String>, CsvMetadata> {

    private final NamesCsvImport service;

    NamesController(NamesCsvImport service) {
        this.service = service;
    }

    @Override
    public BulkImportProvider<?, List<String>, CsvMetadata, ?> getBulkImportService() {
        return service;
    }
}
