package de.szut.pms.hello;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import de.szut.pms.testcontainers.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeleteTest extends AbstractIntegrationTest {

    @Test
    @WithMockUser
    void deleteRemovesTheEntry() throws Exception {
        HelloEntity saved = helloRepository.save(new HelloEntity("Foo"));

        mockMvc.perform(delete("/hello/{id}", saved.getId()).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(helloRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @WithMockUser
    void deleteOfUnknownIdReturns404() throws Exception {
        mockMvc.perform(delete("/hello/{id}", 999L).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
