package com.peluware.freddy.cruder.springframework.web.export;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

class OwnedExportControllerTest {

    private TeamPersonProvider people;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        people = new TeamPersonProvider();
        mvc = MockMvcBuilders.standaloneSetup(new TeamsController(new TeamPersonCsvExport(people)))
            .setCustomArgumentResolvers(new SortHandlerMethodArgumentResolver())
            .build();
    }

    private MockHttpServletResponse download(String url) throws Exception {
        var started = mvc.perform(get(url)).andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(started));
        return started.getResponse();
    }

    @Test
    void downloadsOnlyTheRecordsOfTheOwnerInThePath() throws Exception {
        people.create(7L, new TeamPersonInput("Ana"));
        people.create(9L, new TeamPersonInput("Luis"));

        var response = download("/teams/7/people/export");

        assertEquals(200, response.getStatus());
        assertEquals("Name\r\nAna\r\n", response.getContentAsString());
    }
}
