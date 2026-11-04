package de.szut.pms.project;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import de.szut.pms.customer.CustomerClient;
import de.szut.pms.employee.EmployeeClient;
import de.szut.pms.testcontainers.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectModificationNotFoundTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @MockitoBean
    private EmployeeClient employeeClient;

    @MockitoBean
    private CustomerClient customerClient;

    private Long existingProjectId;
    private Long unknownProjectId;

    @BeforeEach
    void prepareProjects() {
        projectRepository.deleteAll();

        ProjectEntity project = new ProjectEntity(
                "Kundenportal", 1L, 2L, "Anna Müller", "Bestehendes Projektziel",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 15));
        project.addEmployee(11L, "Java");

        existingProjectId = projectRepository.save(project).getId();
        unknownProjectId = existingProjectId + 1;
    }

    @Test
    void updateOfUnknownProjectReturns404WithoutChangingStoredProjects() throws Exception {
        String request = """
                {
                  "name": "Geändertes Projekt",
                  "responsibleEmployeeId": 99,
                  "customerId": 99,
                  "customerContactName": "Maria Beispiel",
                  "comment": "Neues Projektziel",
                  "startDate": "2026-11-02",
                  "endDate": "2026-12-16"
                }
                """;

        expectNotFound(mockMvc.perform(
                put("/projects/{id}", unknownProjectId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)));

        assertStoredProjectUnchanged();
        verifyNoInteractions(employeeClient, customerClient);
    }

    @Test
    void deleteOfUnknownProjectReturns404WithoutRemovingStoredProjects() throws Exception {
        expectNotFound(mockMvc.perform(
                delete("/projects/{id}", unknownProjectId).with(jwt())));

        assertStoredProjectUnchanged();
        verifyNoInteractions(employeeClient, customerClient);
    }

    private void expectNotFound(ResultActions response) throws Exception {
        response
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Projekt nicht gefunden"))
                .andExpect(jsonPath("$.detail").value(
                        "Projekt mit der ID " + unknownProjectId + " wurde nicht gefunden."))
                .andExpect(jsonPath("$.instance").value("/projects/" + unknownProjectId));
    }

    private void assertStoredProjectUnchanged() {
        assertThat(projectRepository.count()).isEqualTo(1);

        ProjectEntity stored = projectRepository.findById(existingProjectId).orElseThrow();
        assertThat(stored.getName()).isEqualTo("Kundenportal");
        assertThat(stored.getResponsibleEmployeeId()).isEqualTo(1L);
        assertThat(stored.getCustomerId()).isEqualTo(2L);
        assertThat(stored.getCustomerContactName()).isEqualTo("Anna Müller");
        assertThat(stored.getComment()).isEqualTo("Bestehendes Projektziel");
        assertThat(stored.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(stored.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 15));
        assertThat(stored.getEmployees())
                .extracting(ProjectEmployeeEntity::getEmployeeId)
                .containsExactly(11L);
        assertThat(stored.getEmployees())
                .extracting(ProjectEmployeeEntity::getQualification)
                .containsExactly("Java");
    }
}
