package com.peluware.freddy.cruder.springframework.web.export;

import com.peluware.freddy.cruder.CrudContext;
import com.peluware.freddy.cruder.CrudOptions;
import com.peluware.freddy.cruder.export.Export;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;

/**
 * What the export controllers share: answering with a resolved {@link Export}.
 */
final class ExportResponses {

    private ExportResponses() {
    }

    /**
     * Answers with the export written straight to the response, in the context of the request.
     */
    static ResponseEntity<StreamingResponseBody> response(Export resolved, CrudOptions options) {
        StreamingResponseBody body = out -> CrudContext.run(options, () -> resolved.writeTo(out));
        var disposition = ContentDisposition.attachment().filename(resolved.filename(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .contentType(MediaType.parseMediaType(resolved.mediaType()))
            .body(body);
    }
}
