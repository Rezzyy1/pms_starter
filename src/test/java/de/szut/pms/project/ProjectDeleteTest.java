package de.szut.pms.project;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.szut.pms.customer.CustomerClient;
import de.szut.pms.employee.EmployeeClient;
import de.szut.pms.testcontainers.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectDeleteTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmployeeClient employeeClient;

    @MockitoBean
    private CustomerClient customerClient;

    @BeforeEach
    void prepareProjects() {
        projectRepository.deleteAll();
    }

    @Test
    void deleteRemovesProjectAndAssignmentsWithoutChangingAnotherProject() throws Exception {
        ProjectEntity project = new ProjectEntity(
                "Kundenportal", 1L, 2L, "Anna Müller", "Entwicklung eines Kundenportals",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 15));
        project.addEmployee(11L, "Java");
        project.addEmployee(12L, "SQL");
        Long projectId = projectRepository.save(project).getId();

        ProjectEntity otherProject = new ProjectEntity(
                "Interne Verwaltung", 3L, 4L, "Max Mustermann", "Unverändertes Projektziel",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31));
        otherProject.addEmployee(21L, "Consulting");
        Long otherProjectId = projectRepository.save(otherProject).getId();

        assertThat(countAssignments(projectId)).isEqualTo(2);
        assertThat(countAssignments(otherProjectId)).isEqualTo(1);

        mockMvc.perform(delete("/projects/{id}", projectId).with(jwt()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(projectRepository.findById(projectId)).isEmpty();
        assertThat(countAssignments(projectId)).isZero();
        assertThat(projectRepository.count()).isEqualTo(1);

        ProjectEntity remaining = projectRepository.findById(otherProjectId).orElseThrow();
        assertThat(remaining.getName()).isEqualTo("Interne Verwaltung");
        assertThat(remaining.getResponsibleEmployeeId()).isEqualTo(3L);
        assertThat(remaining.getCustomerId()).isEqualTo(4L);
        assertThat(remaining.getCustomerContactName()).isEqualTo("Max Mustermann");
        assertThat(remaining.getComment()).isEqualTo("Unverändertes Projektziel");
        assertThat(remaining.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(remaining.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(remaining.getEmployees())
                .extracting(
                        ProjectEmployeeEntity::getEmployeeId,
                        ProjectEmployeeEntity::getQualification)
                .containsExactly(tuple(21L, "Consulting"));
        assertThat(countAssignments(otherProjectId)).isEqualTo(1);

        verifyNoInteractions(employeeClient, customerClient);
    }

    private long countAssignments(Long projectId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project_employees WHERE project_id = ?",
                Long.class,
                projectId);
    }
}
