package com.peluware.freddy.cruder.springframework.web.export;

import com.peluware.freddy.cruder.export.ExportProvider;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/people")
class PeopleController implements ExportController {

    private final PersonCsvExport export;

    PeopleController(PersonCsvExport export) {
        this.export = export;
    }

    @Override
    public ExportProvider getExportService() {
        return export;
    }
}
