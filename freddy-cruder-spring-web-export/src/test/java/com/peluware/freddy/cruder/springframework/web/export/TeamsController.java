package com.peluware.freddy.cruder.springframework.web.export;

import com.peluware.freddy.cruder.export.OwnedExportProvider;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teams/{ownerId}/people")
class TeamsController implements OwnedExportController<Long> {

    private final TeamPersonCsvExport export;

    TeamsController(TeamPersonCsvExport export) {
        this.export = export;
    }

    @Override
    public OwnedExportProvider<Long> getExportService() {
        return export;
    }
}
