package com.peluware.freddy.cruder.bulkimport.excel;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

class OwnedStreamingExcelBulkImportProviderTest extends AbstractOwnedExcelBulkImportProviderTest {

    @Override
    protected AbstractOwnedExcelBulkImportProvider<Long, MemberInput, Long> importer() {
        return StreamingMemberImport.of(members);
    }

    @Override
    protected AbstractOwnedExcelBulkImportProvider<Long, MemberInput, Long> importerSkipping(BiPredicate<Long, ExcelRow> skip) {
        return StreamingMemberImport.skipping(members, skip);
    }

    @Override
    protected AbstractOwnedExcelBulkImportProvider<Long, MemberInput, Long> importerObserving(BiConsumer<Long, Long> created) {
        return StreamingMemberImport.observing(members, created);
    }
}
