package com.peluware.freddy.cruder.springframework.web.bulkimport;

import com.peluware.freddy.cruder.CrudContext;
import com.peluware.freddy.cruder.CrudOptions;
import com.peluware.freddy.cruder.bulkimport.ImportTemplate;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * What the bulk import controllers share: reading an uploaded file and answering with a template.
 */
final class BulkImportResponses {

    private BulkImportResponses() {
    }

    static <T> T readingFile(MultipartFile file, Function<InputStream, T> action) {
        try (var in = file.getInputStream()) {
            return action.apply(in);
        } catch (IOException e) {
            throw new UncheckedIOException("The file could not be read", e);
        }
    }

    /**
     * Answers with the template written straight to the response, in the context of the request.
     */
    static ResponseEntity<StreamingResponseBody> template(ImportTemplate template, CrudOptions options) {
        StreamingResponseBody body = out -> CrudContext.run(options, () -> template.writeTo(out));
        var disposition = ContentDisposition.attachment().filename(template.name(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .contentType(MediaType.parseMediaType(template.mediaType()))
            .body(body);
    }
}
