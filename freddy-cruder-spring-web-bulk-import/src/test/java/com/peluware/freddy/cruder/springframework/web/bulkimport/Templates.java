package com.peluware.freddy.cruder.springframework.web.bulkimport;

import com.peluware.freddy.cruder.bulkimport.ImportTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

final class Templates {

    private Templates() {
    }

    /**
     * A template that writes the given text.
     */
    static ImportTemplate.Writer text(String content) {
        return out -> {
            try {
                out.write(content.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }
}
