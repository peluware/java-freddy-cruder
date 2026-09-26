package com.peluware.freddy.cruder.springframework.web.bulkimport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

class OwnedBulkImportControllerTest {

    private List<String> log;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        log = new ArrayList<>();
        mvc = MockMvcBuilders.standaloneSetup(new TeamNamesController(new TeamNamesCsvImport(log))).build();
    }

    private static MockMultipartFile file(String content) {
        return new MockMultipartFile("file", "nombres.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void importsTheUploadedFileForTheOwnerInThePath() throws Exception {
        var response = mvc.perform(multipart("/teams/7/names/import").file(file("nombre\nAna\nLuis\n")).param("flag", "yes")).andReturn().getResponse();

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"created\":2"));
        assertEquals(List.of("7:Ana|yes", "7:Luis|yes"), log);
    }

    @Test
    void previewsTheUploadedFileForTheOwnerWithoutCreatingAnything() throws Exception {
        var response = mvc.perform(multipart("/teams/7/names/import/preview").file(file("nombre\nAna\n"))).andReturn().getResponse();

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"created\":1"));
        assertTrue(log.isEmpty());
    }

    @Test
    void downloadsTheTemplateOfTheOwner() throws Exception {
        var started = mvc.perform(get("/teams/7/names/import/template")).andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(started));
        var response = started.getResponse();

        assertEquals(200, response.getStatus());
        var disposition = response.getHeader("Content-Disposition");
        assertNotNull(disposition);
        assertTrue(disposition.contains("nombres-7.csv"));
        assertEquals("nombre\n", response.getContentAsString());
    }
}
