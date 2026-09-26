package com.peluware.freddy.cruder.bulkimport.csv;

import com.peluware.freddy.cruder.bulkimport.ImportConversion;
import com.peluware.freddy.cruder.bulkimport.ImportTemplate;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * Imports the members of a team from a CSV file with the column {@code nombre}. The name of each
 * member is followed by {@code @} and the team, so that a test can see that the owner reached the
 * conversion.
 */
class MemberCsvImport extends OwnedCsvBulkImportProvider<Long, MemberInput, Long> {

    private final BiPredicate<Long, CsvRow> skip;
    private final BiConsumer<Long, Long> created;

    private MemberCsvImport(
        MemberProvider members,
        BiPredicate<Long, CsvRow> skip,
        BiConsumer<Long, Long> created
    ) {
        super(members);
        this.skip = skip;
        this.created = created;
    }

    static MemberCsvImport of(MemberProvider members) {
        return new MemberCsvImport(members, (ownerId, row) -> false, (ownerId, id) -> {
        });
    }

    static MemberCsvImport skipping(MemberProvider members, BiPredicate<Long, CsvRow> skip) {
        return new MemberCsvImport(members, skip, (ownerId, id) -> {
        });
    }

    static MemberCsvImport observing(MemberProvider members, BiConsumer<Long, Long> created) {
        return new MemberCsvImport(members, (ownerId, row) -> false, created);
    }

    @Override
    protected ImportConversion<MemberInput> convert(Long ownerId, CsvRow row) {
        if (skip.test(ownerId, row)) {
            return ImportConversion.skipped("Team is closed");
        }
        var input = new MemberInput();
        var binder = new CsvRowBinder(row);
        binder.required(name -> input.setName(name + "@" + ownerId), 0, "nombre", CsvCell::textOrNull, "El nombre es obligatorio");
        return binder.toConversion(input);
    }

    @Override
    protected void created(Long ownerId, Long output) {
        created.accept(ownerId, output);
    }

    @Override
    public ImportTemplate template(Long ownerId) {
        return new ImportTemplate("miembros-" + ownerId + ".csv", "text/csv", out -> {
        });
    }
}
