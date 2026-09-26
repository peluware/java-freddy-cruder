package com.peluware.freddy.cruder.springframework.web.bulkimport;

import com.peluware.freddy.cruder.CrudContext;
import com.peluware.freddy.cruder.bulkimport.BulkImportPreview;
import com.peluware.freddy.cruder.bulkimport.BulkImportProvider;
import com.peluware.freddy.cruder.bulkimport.BulkImportResult;
import com.peluware.freddy.cruder.springframework.SpringCrudOptions;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Exposes a {@link BulkImportProvider} to import a file, preview it and download its template.
 *
 * @param <PREVIEW>      the type of the data of each record, as shown in the preview
 * @param <PREVIEW_META> the type of the metadata of the file, as shown in the preview
 */
public interface BulkImportController<PREVIEW, PREVIEW_META> {

    BulkImportProvider<?, PREVIEW, PREVIEW_META, ?> getBulkImportService();

    @PostMapping("/import")
    default ResponseEntity<BulkImportResult> importFile(
        @RequestParam("file") MultipartFile file,
        @RequestParam MultiValueMap<String, String> parameters
    ) {
        var options = SpringCrudOptions.of(parameters);
        return ResponseEntity.ok(CrudContext.call(options, () -> BulkImportResponses.readingFile(file, in -> getBulkImportService().execute(in))));
    }

    @PostMapping("/import/preview")
    default ResponseEntity<BulkImportPreview<PREVIEW, PREVIEW_META>> preview(
        @RequestParam("file") MultipartFile file,
        @RequestParam MultiValueMap<String, String> parameters
    ) {
        var options = SpringCrudOptions.of(parameters);
        return ResponseEntity.ok(CrudContext.call(options, () -> BulkImportResponses.readingFile(file, in -> getBulkImportService().preview(in))));
    }

    @GetMapping("/import/template")
    default ResponseEntity<StreamingResponseBody> template(@RequestParam MultiValueMap<String, String> parameters) {
        var options = SpringCrudOptions.of(parameters);
        var template = CrudContext.call(options, () -> getBulkImportService().template());
        return BulkImportResponses.template(template, options);
    }
}
