package com.peluware.freddy.cruder.bulkimport.excel;

import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Opens workbooks by loading the whole file into memory, evaluating formulas.
 */
final class ClassicWorkbooks {

    private ClassicWorkbooks() {
    }

    static Workbook open(InputStream in) {
        try {
            return WorkbookFactory.create(in);
        } catch (IOException e) {
            throw new UncheckedIOException("The file could not be read", e);
        }
    }

    static FormulaEvaluator evaluatorOf(Workbook workbook) {
        return workbook.getCreationHelper().createFormulaEvaluator();
    }
}
