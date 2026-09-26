package com.peluware.freddy.cruder.bulkimport.excel;

import com.peluware.freddy.cruder.bulkimport.BulkImportResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import static com.peluware.freddy.cruder.bulkimport.excel.MemberRows.SHEET;
import static com.peluware.freddy.cruder.bulkimport.excel.Workbooks.names;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the owned variant does with the owner: it reaches the conversion, the callback
 * after creating and the template, and every record is created for it. Each subclass says which
 * provider to read with.
 */
abstract class AbstractOwnedExcelBulkImportProviderTest {

    protected MemberProvider members;

    @BeforeEach
    void setUp() {
        members = new MemberProvider();
    }

    protected abstract AbstractOwnedExcelBulkImportProvider<Long, MemberInput, Long> importer();

    protected abstract AbstractOwnedExcelBulkImportProvider<Long, MemberInput, Long> importerSkipping(BiPredicate<Long, ExcelRow> skip);

    protected abstract AbstractOwnedExcelBulkImportProvider<Long, MemberInput, Long> importerObserving(BiConsumer<Long, Long> created);

    @Test
    void createsEachRowForTheOwnerItIsImportedFor() {
        var importer = importer();

        importer.execute(1L, names(SHEET, "Ana", "Luis"));
        importer.execute(2L, names(SHEET, "Eva"));

        assertEquals(List.of(1L, 1L, 2L), members.entities().stream().map(member -> member.teamId).toList());
    }

    @Test
    void theOwnerReachesTheConversion() {
        importer().execute(7L, names(SHEET, "Ana"));

        assertEquals("Ana@7", members.entities().getFirst().name);
    }

    @Test
    void theOwnerReachesTheConversionThatSkips() {
        var importer = importerSkipping((ownerId, row) -> ownerId == 2L);

        var open = importer.execute(1L, names(SHEET, "Ana"));
        var closed = importer.execute(2L, names(SHEET, "Eva"));

        assertEquals(new BulkImportResult(1, 0), open);
        assertEquals(new BulkImportResult(0, 1), closed);
    }

    @Test
    void theOwnerReachesTheCallbackAfterCreating() {
        var seen = new ArrayList<String>();
        var importer = importerObserving((ownerId, id) -> seen.add(ownerId + ":" + id));

        importer.execute(3L, names(SHEET, "Ana", "Luis"));

        assertEquals(List.of("3:1", "3:2"), seen);
    }

    @Test
    void previewSkipsForTheOwnerAndCreatesNothing() {
        var importer = importerSkipping((ownerId, row) -> ownerId == 2L);

        var preview = importer.preview(2L, names(SHEET, "Eva"));

        assertEquals(0, preview.created());
        assertEquals(1, preview.skipped());
        assertTrue(members.entities().isEmpty());
    }

    @Test
    void theTemplateIsBuiltForTheOwner() {
        assertEquals("miembros-5.xlsx", importer().template(5L).name());
    }
}
