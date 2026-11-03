package de.szut.pms.project;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.szut.pms.customer.CustomerClient;
import de.szut.pms.employee.EmployeeClient;
import de.szut.pms.testcontainers.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectUpdateTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @MockitoBean
    private EmployeeClient employeeClient;

    @MockitoBean
    private CustomerClient customerClient;

    @BeforeEach
    void prepareProjects() {
        projectRepository.deleteAll();
    }

    @Test
    void updateStoresAllChangesAndPreservesProjectAndEmployees() throws Exception {
        ProjectEntity project = new ProjectEntity(
                "Altes Kundenportal", 1L, 2L, "Anna Müller", "Altes Projektziel",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 15));
        project.addEmployee(11L, "Java");
        project.addEmployee(12L, "SQL");
        Long projectId = projectRepository.save(project).getId();

        when(employeeClient.existsById(99L, "test-token"))
                .thenReturn(true);
        when(customerClient.existsById(88L))
                .thenReturn(true);

        String request = """
                {
                  "name": "Neues Kundenportal",
                  "responsibleEmployeeId": 99,
                  "customerId": 88,
                  "customerContactName": "Maria Beispiel",
                  "comment": "Neues Projektziel",
                  "startDate": "2026-11-10",
                  "endDate": "2026-12-31"
                }
                """;

        mockMvc.perform(put("/projects/{id}", projectId)
                        .with(jwt().jwt(builder -> builder.tokenValue("test-token")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        ProjectEntity stored = projectRepository.findById(projectId).orElseThrow();

        assertThat(projectRepository.count()).isEqualTo(1);
        assertThat(stored.getId()).isEqualTo(projectId);
        assertThat(stored.getName()).isEqualTo("Neues Kundenportal");
        assertThat(stored.getResponsibleEmployeeId()).isEqualTo(99L);
        assertThat(stored.getCustomerId()).isEqualTo(88L);
        assertThat(stored.getCustomerContactName()).isEqualTo("Maria Beispiel");
        assertThat(stored.getComment()).isEqualTo("Neues Projektziel");
        assertThat(stored.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(stored.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(stored.getEmployees())
                .extracting(
                        ProjectEmployeeEntity::getEmployeeId,
                        ProjectEmployeeEntity::getQualification)
                .containsExactlyInAnyOrder(
                        tuple(11L, "Java"),
                        tuple(12L, "SQL"));

        verify(employeeClient).existsById(99L, "test-token");
        verify(customerClient).existsById(88L);
    }
}
