package de.szut.pms.hello;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import de.szut.pms.testcontainers.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GetByMessageTest extends AbstractIntegrationTest {

    @Test
    @WithMockUser
    void findsOnlyEntriesWithMatchingMessage() throws Exception {
        helloRepository.save(new HelloEntity("Foo"));
        helloRepository.save(new HelloEntity("Bar"));

        mockMvc.perform(get("/hello/findByMessage").param("message", "Foo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].message").value("Foo"));
    }

    @Test
    @WithMockUser
    void returnsEmptyListForUnknownMessage() throws Exception {
        mockMvc.perform(get("/hello/findByMessage").param("message", "unbekannt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
