package com.peluware.freddy.cruder.bulkimport.csv;

import com.peluware.freddy.cruder.bulkimport.BulkImportResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the owned variant does with the owner: it reaches the conversion, the callback
 * after creating and the template, and every record is created for it.
 */
class OwnedCsvBulkImportProviderTest {

    private MemberProvider members;

    @BeforeEach
    void setUp() {
        members = new MemberProvider();
    }

    private static InputStream csv(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void createsEachRecordForTheOwnerItIsImportedFor() {
        var importer = MemberCsvImport.of(members);

        importer.execute(1L, csv("nombre\nAna\nLuis\n"));
        importer.execute(2L, csv("nombre\nEva\n"));

        assertEquals(List.of(1L, 1L, 2L), members.entities().stream().map(member -> member.teamId).toList());
    }

    @Test
    void theOwnerReachesTheConversion() {
        MemberCsvImport.of(members).execute(7L, csv("nombre\nAna\n"));

        assertEquals("Ana@7", members.entities().getFirst().name);
    }

    @Test
    void theOwnerReachesTheConversionThatSkips() {
        var importer = MemberCsvImport.skipping(members, (ownerId, row) -> ownerId == 2L);

        var open = importer.execute(1L, csv("nombre\nAna\n"));
        var closed = importer.execute(2L, csv("nombre\nEva\n"));

        assertEquals(new BulkImportResult(1, 0), open);
        assertEquals(new BulkImportResult(0, 1), closed);
    }

    @Test
    void theOwnerReachesTheCallbackAfterCreating() {
        var seen = new ArrayList<String>();
        var importer = MemberCsvImport.observing(members, (ownerId, id) -> seen.add(ownerId + ":" + id));

        importer.execute(3L, csv("nombre\nAna\nLuis\n"));

        assertEquals(List.of("3:1", "3:2"), seen);
    }

    @Test
    void previewSkipsForTheOwnerAndCreatesNothing() {
        var importer = MemberCsvImport.skipping(members, (ownerId, row) -> ownerId == 2L);

        var preview = importer.preview(2L, csv("nombre\nEva\n"));

        assertEquals(0, preview.created());
        assertEquals(1, preview.skipped());
        assertTrue(members.entities().isEmpty());
    }

    @Test
    void theTemplateIsBuiltForTheOwner() {
        assertEquals("miembros-5.csv", MemberCsvImport.of(members).template(5L).name());
    }
}
