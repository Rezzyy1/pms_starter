package de.szut.pms.hello;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import de.szut.pms.testcontainers.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostTest extends AbstractIntegrationTest {

    @Test
    void createWithoutAuthenticationIsRejected() throws Exception {
        final String content = """
                {
                    "message": "Foo"
                }
                """;

        mockMvc.perform(post("/hello")
                        .content(content).contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void createStoresAndReturnsTheEntry() throws Exception {
        final String content = """
                {
                    "message": "Foo"
                }
                """;

        mockMvc.perform(post("/hello")
                        .content(content).contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("id").exists())
                .andExpect(jsonPath("message", is("Foo")));

        List<HelloEntity> stored = helloRepository.findAllByMessage("Foo");
        assertThat(stored).hasSize(1);
    }

    @Test
    @WithMockUser
    void createWithTooShortMessageIsRejected() throws Exception {
        final String content = """
                {
                    "message": "ab"
                }
                """;

        mockMvc.perform(post("/hello")
                        .content(content).contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors.message").exists());
    }
}
