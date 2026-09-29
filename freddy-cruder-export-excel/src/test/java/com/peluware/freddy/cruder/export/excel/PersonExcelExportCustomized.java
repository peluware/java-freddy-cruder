package com.peluware.freddy.cruder.export.excel;

import com.peluware.freddy.cruder.export.ExportField;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.List;

class PersonExcelExportCustomized extends ExcelExportProvider<PersonOutput> {

    private static final List<ExportField<PersonOutput>> FIELDS = List.of(
        new ExportField<>("name", "Name", PersonOutput::name),
        new ExportField<>("age", "Age", PersonOutput::age)
    );

    PersonExcelExportCustomized(PersonProvider provider) {
        super(provider);
    }

    @Override
    protected String filename() {
        return "people.xlsx";
    }

    @Override
    protected List<ExportField<PersonOutput>> fields() {
        return FIELDS;
    }

    @Override
    protected void writeWorkbook(Workbook workbook, List<ExportField<PersonOutput>> fields, List<PersonOutput> records) {
        var font = workbook.createFont();
        font.setBold(true);
        var boldStyle = workbook.createCellStyle();
        boldStyle.setFont(font);

        CellStyler styler = (cell, _, _, header) -> {
            if (header) {
                cell.setCellStyle(boldStyle);
            }
        };

        ExcelWriting.write(workbook, "Sheet1", 1, 1, fields, records.stream(), styler);
    }
}
