package com.peluware.freddy.cruder.springframework.web.export;

import com.peluware.freddy.cruder.export.UnknownExportFieldsException;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

class ExportControllerTest {

    private PersonProvider people;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        people = new PersonProvider();
        mvc = MockMvcBuilders.standaloneSetup(new PeopleController(new PersonCsvExport(people)))
            .setCustomArgumentResolvers(new SortHandlerMethodArgumentResolver())
            .build();
    }

    private MockHttpServletResponse download(String url) throws Exception {
        var started = mvc.perform(get(url)).andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(started));
        return started.getResponse();
    }

    @Test
    void downloadsEveryRecordAsAnAttachment() throws Exception {
        people.create(new PersonInput("Ana", 30));
        people.create(new PersonInput("Luis", 25));

        var response = download("/people/export");

        assertEquals(200, response.getStatus());
        assertEquals("text/csv", response.getContentType());
        var disposition = response.getHeader("Content-Disposition");
        assertNotNull(disposition);
        assertTrue(disposition.contains("attachment"));
        assertTrue(disposition.contains("people.csv"));
        assertEquals("Name,Age\r\nAna,30\r\nLuis,25\r\n", response.getContentAsString());
    }

    @Test
    void honorsTheRequestedFieldSelection() throws Exception {
        people.create(new PersonInput("Ana", 30));

        var response = download("/people/export?fields=age");

        assertEquals("Age\r\n30\r\n", response.getContentAsString());
    }

    @Test
    void rejectsFieldsTheExportDoesNotOffer() {
        var exception = assertThrows(ServletException.class,
            () -> mvc.perform(get("/people/export").param("fields", "email")));

        assertInstanceOf(UnknownExportFieldsException.class, exception.getCause());
    }
}
