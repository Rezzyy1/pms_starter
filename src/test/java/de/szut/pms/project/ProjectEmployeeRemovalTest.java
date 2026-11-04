package de.szut.pms.project;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import de.szut.pms.employee.EmployeeClient;
import de.szut.pms.testcontainers.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectEmployeeRemovalTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmployeeClient employeeClient;

    private Long projectId;
    private Long otherProjectId;

    @BeforeEach
    void prepareProjects() {
        projectRepository.deleteAll();

        ProjectEntity project = new ProjectEntity(
                "Kundenportal", 1L, 2L, "Anna Müller", "Bestehendes Projektziel",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 15));
        project.addEmployee(11L, "Java");
        project.addEmployee(12L, "SQL");
        projectId = projectRepository.save(project).getId();

        ProjectEntity otherProject = new ProjectEntity(
                "Vorheriges Projekt", 3L, 4L, "Max Mustermann", "Anderes Projektziel",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        otherProject.addEmployee(11L, "Consulting");
        otherProject.addEmployee(21L, "SQL");
        otherProjectId = projectRepository.save(otherProject).getId();
    }

    @AfterEach
    void employeeServiceIsNotCalledForRemoval() {
        verifyNoInteractions(employeeClient);
    }

    @Test
    void removalDeletesOnlyTheRequestedAssignmentAndReturnsItsData() throws Exception {
        removeAssignedEmployee(11L, "Java");

        ProjectEntity stored = projectRepository.findById(projectId).orElseThrow();
        assertThat(stored.getEmployees())
                .extracting(
                        ProjectEmployeeEntity::getEmployeeId,
                        ProjectEmployeeEntity::getQualification)
                .containsExactly(tuple(12L, "SQL"));

        ProjectEntity other = projectRepository.findById(otherProjectId).orElseThrow();
        assertThat(other.getEmployees())
                .extracting(
                        ProjectEmployeeEntity::getEmployeeId,
                        ProjectEmployeeEntity::getQualification)
                .containsExactlyInAnyOrder(
                        tuple(11L, "Consulting"),
                        tuple(21L, "SQL"));
    }

    @Test
    void removalOfTheLastEmployeeLeavesTheProjectWithAnEmptyEmployeeList() throws Exception {
        removeAssignedEmployee(12L, "SQL");
        removeAssignedEmployee(11L, "Java");

        ProjectEntity stored = projectRepository.findById(projectId).orElseThrow();
        assertThat(stored.getEmployees()).isEmpty();
        assertThat(projectRepository.count()).isEqualTo(2);
    }

    @Test
    void removalFromAnUnknownProjectReturns404() throws Exception {
        Long unknownProjectId = Math.max(projectId, otherProjectId) + 1;

        expectProblem(unknownProjectId, 11L, 404)
                .andExpect(jsonPath("$.title").value("Projekt nicht gefunden"))
                .andExpect(jsonPath("$.detail").value(
                        "Projekt mit der ID " + unknownProjectId + " wurde nicht gefunden."));
    }

    @ParameterizedTest
    @ValueSource(longs = {21L, 999L, 0L, -1L})
    void removalOfAnEmployeeNotAssignedToTheProjectReturns404(long employeeId) throws Exception {
        expectProblem(projectId, employeeId, 404)
                .andExpect(jsonPath("$.detail").value(
                        "Mitarbeiter mit der ID " + employeeId
                                + " ist dem Projekt mit der ID " + projectId + " nicht zugeordnet."));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void removalFromANonPositiveProjectIdReturns404(long requestedProjectId) throws Exception {
        expectProblem(requestedProjectId, 11L, 404)
                .andExpect(jsonPath("$.detail").value(
                        "Projekt mit der ID " + requestedProjectId + " wurde nicht gefunden."));
    }

    @Test
    void repeatedRemovalReturns404WithoutFurtherChanges() throws Exception {
        removeAssignedEmployee(11L, "Java");

        expectProblem(projectId, 11L, 404)
                .andExpect(jsonPath("$.detail").value(
                        "Mitarbeiter mit der ID 11 ist dem Projekt mit der ID "
                                + projectId + " nicht zugeordnet."));
    }

    @ParameterizedTest
    @CsvSource({
            "project, abc",
            "project, 9223372036854775808",
            "employee, abc",
            "employee, 9223372036854775808"
    })
    void removalWithAMalformedIdReturns400(String field, String id) throws Exception {
        Object requestedProjectId = field.equals("project") ? id : projectId;
        Object requestedEmployeeId = field.equals("employee") ? id : 11L;

        expectProblem(requestedProjectId, requestedEmployeeId, 400);
    }

    @Test
    void removalWithoutAuthenticationReturns401() throws Exception {
        rejectedRemoval(delete("/projects/{projectId}/employees/{employeeId}", projectId, 11L))
                .andExpect(status().isUnauthorized());
    }

    private void removeAssignedEmployee(Long employeeId, String qualification) throws Exception {
        List<Map<String, Object>> projectsBefore = jdbcTemplate.queryForList(
                "SELECT * FROM projects ORDER BY id");
        List<Map<String, Object>> assignmentsToKeep = jdbcTemplate.queryForList(
                """
                SELECT * FROM project_employees
                WHERE NOT (project_id = ? AND employee_id = ?)
                ORDER BY id
                """, projectId, employeeId);

        assertThat(countAssignments(projectId, employeeId)).isEqualTo(1);

        mockMvc.perform(delete("/projects/{projectId}/employees/{employeeId}", projectId, employeeId)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.employeeId").value(employeeId))
                .andExpect(jsonPath("$.qualification").value(qualification));

        assertThat(countAssignments(projectId, employeeId)).isZero();
        assertThat(jdbcTemplate.queryForList("SELECT * FROM projects ORDER BY id"))
                .isEqualTo(projectsBefore);
        assertThat(jdbcTemplate.queryForList("SELECT * FROM project_employees ORDER BY id"))
                .isEqualTo(assignmentsToKeep);
    }

    private ResultActions expectProblem(Object requestedProjectId, Object employeeId, int expectedStatus)
            throws Exception {
        return rejectedRemoval(delete(
                "/projects/{projectId}/employees/{employeeId}", requestedProjectId, employeeId).with(jwt()))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").value(
                        "/projects/" + requestedProjectId + "/employees/" + employeeId));
    }

    private ResultActions rejectedRemoval(MockHttpServletRequestBuilder request) throws Exception {
        List<Map<String, Object>> projectsBefore = jdbcTemplate.queryForList(
                "SELECT * FROM projects ORDER BY id");
        List<Map<String, Object>> assignmentsBefore = jdbcTemplate.queryForList(
                "SELECT * FROM project_employees ORDER BY id");

        ResultActions response = mockMvc.perform(request)
                .andExpect(status().is4xxClientError());

        assertThat(jdbcTemplate.queryForList("SELECT * FROM projects ORDER BY id"))
                .isEqualTo(projectsBefore);
        assertThat(jdbcTemplate.queryForList("SELECT * FROM project_employees ORDER BY id"))
                .isEqualTo(assignmentsBefore);

        return response;
    }

    private long countAssignments(Long requestedProjectId, Long employeeId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project_employees WHERE project_id = ? AND employee_id = ?",
                Long.class, requestedProjectId, employeeId);
    }
}
