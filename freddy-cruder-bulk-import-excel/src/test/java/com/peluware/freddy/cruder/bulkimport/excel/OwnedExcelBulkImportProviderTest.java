package com.peluware.freddy.cruder.bulkimport.excel;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

class OwnedExcelBulkImportProviderTest extends AbstractOwnedExcelBulkImportProviderTest {

    @Override
    protected AbstractOwnedExcelBulkImportProvider<Long, MemberInput, Long> importer() {
        return ClassicMemberImport.of(members);
    }

    @Override
    protected AbstractOwnedExcelBulkImportProvider<Long, MemberInput, Long> importerSkipping(BiPredicate<Long, ExcelRow> skip) {
        return ClassicMemberImport.skipping(members, skip);
    }

    @Override
    protected AbstractOwnedExcelBulkImportProvider<Long, MemberInput, Long> importerObserving(BiConsumer<Long, Long> created) {
        return ClassicMemberImport.observing(members, created);
    }
}
