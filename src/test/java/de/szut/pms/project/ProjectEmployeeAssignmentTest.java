package de.szut.pms.project;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.szut.pms.employee.EmployeeClient;
import de.szut.pms.testcontainers.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectEmployeeAssignmentTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmployeeClient employeeClient;

    @BeforeEach
    void prepareProjects() {
        projectRepository.deleteAll();
    }

    @Test
    void assignmentStoresEmployeeAndQualificationWithoutChangingExistingAssignments() throws Exception {
        ProjectEntity project = new ProjectEntity(
                "Kundenportal", 1L, 2L, "Anna Müller", "Entwicklung eines Kundenportals",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 15));
        project.addEmployee(12L, "SQL");
        Long projectId = projectRepository.save(project).getId();

        ProjectEntity previousProject = new ProjectEntity(
                "Vorheriges Projekt", 3L, 4L, "Max Mustermann", "Abgeschlossenes Projektziel",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        previousProject.addEmployee(11L, "Consulting");
        Long previousProjectId = projectRepository.save(previousProject).getId();

        when(employeeClient.hasQualification(11L, "Java", "test-token"))
                .thenReturn(true);

        String request = """
                {
                  "employeeId": 11,
                  "qualification": "Java"
                }
                """;

        mockMvc.perform(post("/projects/{projectId}/employees", projectId)
                        .with(jwt().jwt(builder -> builder.tokenValue("test-token")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.employeeId").value(11))
                .andExpect(jsonPath("$.qualification").value("Java"));

        ProjectEntity stored = projectRepository.findById(projectId).orElseThrow();

        assertThat(projectRepository.count()).isEqualTo(2);
        assertThat(stored.getId()).isEqualTo(projectId);
        assertThat(stored.getName()).isEqualTo("Kundenportal");
        assertThat(stored.getResponsibleEmployeeId()).isEqualTo(1L);
        assertThat(stored.getCustomerId()).isEqualTo(2L);
        assertThat(stored.getCustomerContactName()).isEqualTo("Anna Müller");
        assertThat(stored.getComment()).isEqualTo("Entwicklung eines Kundenportals");
        assertThat(stored.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(stored.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 15));
        assertThat(stored.getEmployees())
                .extracting(
                        ProjectEmployeeEntity::getEmployeeId,
                        ProjectEmployeeEntity::getQualification)
                .containsExactlyInAnyOrder(
                        tuple(12L, "SQL"),
                        tuple(11L, "Java"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT qualification FROM project_employees WHERE project_id = ? AND employee_id = ?",
                String.class, projectId, 11L))
                .isEqualTo("Java");

        ProjectEntity previous = projectRepository.findById(previousProjectId).orElseThrow();
        assertThat(previous.getName()).isEqualTo("Vorheriges Projekt");
        assertThat(previous.getResponsibleEmployeeId()).isEqualTo(3L);
        assertThat(previous.getCustomerId()).isEqualTo(4L);
        assertThat(previous.getCustomerContactName()).isEqualTo("Max Mustermann");
        assertThat(previous.getComment()).isEqualTo("Abgeschlossenes Projektziel");
        assertThat(previous.getStartDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(previous.getEndDate()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(previous.getEmployees())
                .extracting(
                        ProjectEmployeeEntity::getEmployeeId,
                        ProjectEmployeeEntity::getQualification)
                .containsExactly(tuple(11L, "Consulting"));

        verify(employeeClient).hasQualification(11L, "Java", "test-token");
    }
}
