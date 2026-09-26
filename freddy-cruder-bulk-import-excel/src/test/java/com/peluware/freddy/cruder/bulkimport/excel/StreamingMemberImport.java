package com.peluware.freddy.cruder.bulkimport.excel;

import com.peluware.freddy.cruder.bulkimport.ImportConversion;
import com.peluware.freddy.cruder.bulkimport.ImportTemplate;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * Imports the members of a team with {@link OwnedStreamingExcelBulkImportProvider}. Which rows are
 * skipped and what {@link #created} does are given by the caller.
 */
class StreamingMemberImport extends OwnedStreamingExcelBulkImportProvider<Long, MemberInput, Long> {

    private final BiPredicate<Long, ExcelRow> skip;
    private final BiConsumer<Long, Long> created;

    private StreamingMemberImport(
        MemberProvider members,
        BiPredicate<Long, ExcelRow> skip,
        BiConsumer<Long, Long> created
    ) {
        super(members);
        this.skip = skip;
        this.created = created;
    }

    static StreamingMemberImport of(MemberProvider members) {
        return new StreamingMemberImport(members, (ownerId, row) -> false, (ownerId, id) -> {
        });
    }

    static StreamingMemberImport skipping(MemberProvider members, BiPredicate<Long, ExcelRow> skip) {
        return new StreamingMemberImport(members, skip, (ownerId, id) -> {
        });
    }

    static StreamingMemberImport observing(MemberProvider members, BiConsumer<Long, Long> created) {
        return new StreamingMemberImport(members, (ownerId, row) -> false, created);
    }

    @Override
    protected String sheetName() {
        return MemberRows.SHEET;
    }

    @Override
    protected ImportConversion<MemberInput> convert(Long ownerId, ExcelRow row) {
        return skip.test(ownerId, row) ? ImportConversion.skipped("Team is closed") : MemberRows.convert(ownerId, row);
    }

    @Override
    protected void created(Long ownerId, Long output) {
        created.accept(ownerId, output);
    }

    @Override
    public ImportTemplate template(Long ownerId) {
        return new ImportTemplate("miembros-" + ownerId + ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out -> {
        });
    }
}
