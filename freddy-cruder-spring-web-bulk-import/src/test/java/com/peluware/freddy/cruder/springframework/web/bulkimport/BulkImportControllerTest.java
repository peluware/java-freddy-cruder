package com.peluware.freddy.cruder.springframework.web.bulkimport;

import com.peluware.freddy.cruder.bulkimport.BulkImportRecordException;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

class BulkImportControllerTest {

    private List<String> log;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        log = new ArrayList<>();
        mvc = MockMvcBuilders.standaloneSetup(new NamesController(new NamesCsvImport(log))).build();
    }

    private static MockMultipartFile file(String content) {
        return new MockMultipartFile("file", "nombres.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void importsTheUploadedFile() throws Exception {
        var response = mvc.perform(multipart("/names/import").file(file("nombre\nAna\nLuis\n"))).andReturn().getResponse();

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"created\":2"));
        assertEquals(List.of("Ana|null", "Luis|null"), log);
    }

    @Test
    void theParametersOfTheRequestReachEachCreation() throws Exception {
        mvc.perform(multipart("/names/import").file(file("nombre\nAna\nLuis\n")).param("flag", "yes"));

        assertEquals(List.of("Ana|yes", "Luis|yes"), log);
    }

    @Test
    void previewsTheUploadedFileWithoutCreatingAnything() throws Exception {
        var response = mvc.perform(multipart("/names/import/preview").file(file("nombre\nAna\n!Mal\n"))).andReturn().getResponse();

        var body = response.getContentAsString();
        assertEquals(200, response.getStatus());
        assertTrue(body.contains("\"created\":1"));
        assertTrue(body.contains("\"rejected\":1"));
        assertTrue(body.contains("\"headers\":[\"nombre\"]"));
        assertTrue(log.isEmpty());
    }

    @Test
    void downloadsTheTemplateAsAnAttachment() throws Exception {
        var response = download("/names/import/template");

        assertEquals(200, response.getStatus());
        assertEquals("text/csv", response.getContentType());
        var disposition = response.getHeader("Content-Disposition");
        assertNotNull(disposition);
        assertTrue(disposition.contains("attachment"));
        assertTrue(disposition.contains("nombres.csv"));
        assertEquals("nombre\n# flag=null", response.getContentAsString());
    }

    @Test
    void theTemplateIsWrittenInTheContextOfTheRequest() throws Exception {
        var response = download("/names/import/template?flag=yes");

        assertEquals("nombre\n# flag=yes", response.getContentAsString());
    }

    /**
     * The template is written to the response on another thread, so the request has to be
     * dispatched again to have it complete.
     */
    private MockHttpServletResponse download(String url) throws Exception {
        var started = mvc.perform(get(url)).andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(started));
        return started.getResponse();
    }

    @Test
    void aFailureIsNotHiddenButLeftForTheApplicationToHandle() {
        var exception = assertThrows(ServletException.class,
            () -> mvc.perform(multipart("/names/import").file(file("nombre\nboom\n"))));

        assertInstanceOf(BulkImportRecordException.class, exception.getCause());
    }
}
