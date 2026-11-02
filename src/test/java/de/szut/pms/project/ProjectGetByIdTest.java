package de.szut.pms.project;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import de.szut.pms.testcontainers.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectGetByIdTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @BeforeEach
    void prepareProjects() {
        projectRepository.deleteAll();
    }

    @Test
    void getByIdReturnsRequestedProjectWithAllData() throws Exception {
        projectRepository.save(project("Interne Verwaltung"));
        ProjectEntity saved = projectRepository.save(project("Kundenportal"));

        mockMvc.perform(get("/projects/{id}", saved.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("Kundenportal"))
                .andExpect(jsonPath("$.responsibleEmployeeId").value(1))
                .andExpect(jsonPath("$.customerId").value(2))
                .andExpect(jsonPath("$.customerContactName").value("Anna Müller"))
                .andExpect(jsonPath("$.comment").value("Entwicklung eines Kundenportals"))
                .andExpect(jsonPath("$.startDate").value("2026-11-01"))
                .andExpect(jsonPath("$.endDate").value("2026-12-15"));

        assertThat(projectRepository.count()).isEqualTo(2);
    }

    @Test
    void getByIdWithUnknownIdIsRejected() throws Exception {
        ProjectEntity saved = projectRepository.save(project("Kundenportal"));
        Long unknownId = saved.getId() + 1;

        mockMvc.perform(get("/projects/{id}", unknownId).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Projekt nicht gefunden"))
                .andExpect(jsonPath("$.detail")
                        .value("Projekt mit der ID " + unknownId + " wurde nicht gefunden."));

        assertThat(projectRepository.count()).isEqualTo(1);
    }

    @Test
    void getByIdWithInvalidIdIsRejected() throws Exception {
        mockMvc.perform(get("/projects/abc").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));

        assertThat(projectRepository.count()).isZero();
    }

    @Test
    void getByIdWithoutAuthenticationIsRejected() throws Exception {
        ProjectEntity saved = projectRepository.save(project("Kundenportal"));

        mockMvc.perform(get("/projects/{id}", saved.getId()))
                .andExpect(status().isUnauthorized());

        assertThat(projectRepository.count()).isEqualTo(1);
    }

    private ProjectEntity project(String name) {
        return new ProjectEntity(
                name, 1L, 2L, "Anna Müller", "Entwicklung eines Kundenportals",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 15));
    }
}
