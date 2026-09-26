package com.peluware.freddy.cruder.bulkimport.excel;

import com.peluware.freddy.cruder.bulkimport.ImportConversion;
import com.peluware.freddy.cruder.bulkimport.ImportTemplate;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * Imports the members of a team with {@link OwnedExcelBulkImportProvider}. Which rows are skipped
 * and what {@link #created} does are given by the caller.
 */
class ClassicMemberImport extends OwnedExcelBulkImportProvider<Long, MemberInput, Long> {

    private final BiPredicate<Long, ExcelRow> skip;
    private final BiConsumer<Long, Long> created;

    private ClassicMemberImport(
        MemberProvider members,
        BiPredicate<Long, ExcelRow> skip,
        BiConsumer<Long, Long> created
    ) {
        super(members);
        this.skip = skip;
        this.created = created;
    }

    static ClassicMemberImport of(MemberProvider members) {
        return new ClassicMemberImport(members, (ownerId, row) -> false, (ownerId, id) -> {
        });
    }

    static ClassicMemberImport skipping(MemberProvider members, BiPredicate<Long, ExcelRow> skip) {
        return new ClassicMemberImport(members, skip, (ownerId, id) -> {
        });
    }

    static ClassicMemberImport observing(MemberProvider members, BiConsumer<Long, Long> created) {
        return new ClassicMemberImport(members, (ownerId, row) -> false, created);
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
