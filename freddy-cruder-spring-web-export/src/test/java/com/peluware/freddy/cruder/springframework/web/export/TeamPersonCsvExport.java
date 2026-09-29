package com.peluware.freddy.cruder.springframework.web.export;

import com.peluware.freddy.cruder.export.ExportField;
import com.peluware.freddy.cruder.export.csv.OwnedCsvExportProvider;

import java.util.List;

class TeamPersonCsvExport extends OwnedCsvExportProvider<Long, TeamPersonOutput> {

    private static final List<ExportField<TeamPersonOutput>> FIELDS = List.of(
        new ExportField<>("name", "Name", TeamPersonOutput::name)
    );

    TeamPersonCsvExport(TeamPersonProvider provider) {
        super(provider);
    }

    @Override
    protected String filename() {
        return "people.csv";
    }

    @Override
    protected List<ExportField<TeamPersonOutput>> fields() {
        return FIELDS;
    }
}
