package com.peluware.freddy.cruder.springframework.web.bulkimport;

import com.peluware.freddy.cruder.CrudContext;
import com.peluware.freddy.cruder.bulkimport.BulkImportPreview;
import com.peluware.freddy.cruder.bulkimport.BulkImportResult;
import com.peluware.freddy.cruder.bulkimport.OwnedBulkImportProvider;
import com.peluware.freddy.cruder.springframework.SpringCrudOptions;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Exposes an {@link OwnedBulkImportProvider} to import a file for an owner, preview it and download
 * its template.
 *
 * @param <OWNER_ID>     the identifier type of the owning resource
 * @param <PREVIEW>      the type of the data of each record, as shown in the preview
 * @param <PREVIEW_META> the type of the metadata of the file, as shown in the preview
 */
public interface OwnedBulkImportController<OWNER_ID, PREVIEW, PREVIEW_META> {

    OwnedBulkImportProvider<OWNER_ID, ?, PREVIEW, PREVIEW_META, ?> getBulkImportService();

    @PostMapping("/import")
    default ResponseEntity<BulkImportResult> importFile(
        @PathVariable("ownerId") OWNER_ID ownerId,
        @RequestParam("file") MultipartFile file,
        @RequestParam MultiValueMap<String, String> parameters
    ) {
        var options = SpringCrudOptions.of(parameters);
        return ResponseEntity.ok(CrudContext.call(options, () -> BulkImportResponses.readingFile(file, in -> getBulkImportService().execute(ownerId, in))));
    }

    @PostMapping("/import/preview")
    default ResponseEntity<BulkImportPreview<PREVIEW, PREVIEW_META>> preview(
        @PathVariable("ownerId") OWNER_ID ownerId,
        @RequestParam("file") MultipartFile file,
        @RequestParam MultiValueMap<String, String> parameters
    ) {
        var options = SpringCrudOptions.of(parameters);
        return ResponseEntity.ok(CrudContext.call(options, () -> BulkImportResponses.readingFile(file, in -> getBulkImportService().preview(ownerId, in))));
    }

    @GetMapping("/import/template")
    default ResponseEntity<StreamingResponseBody> template(
        @PathVariable("ownerId") OWNER_ID ownerId,
        @RequestParam MultiValueMap<String, String> parameters
    ) {
        var options = SpringCrudOptions.of(parameters);
        var template = CrudContext.call(options, () -> getBulkImportService().template(ownerId));
        return BulkImportResponses.template(template, options);
    }
}
