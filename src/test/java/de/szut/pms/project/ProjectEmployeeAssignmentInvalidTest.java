package de.szut.pms.project;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.ObjectMapper;

import de.szut.pms.employee.EmployeeClient;
import de.szut.pms.testcontainers.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectEmployeeAssignmentInvalidTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeClient employeeClient;

    private Long projectId;
    private Long otherProjectId;

    @BeforeEach
    void prepareProjects() {
        projectRepository.deleteAll();

        ProjectEntity project = new ProjectEntity(
                "Kundenportal", 1L, 2L, "Anna Müller", "Bestehendes Projektziel",
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 20));
        project.addEmployee(12L, "SQL");
        projectId = projectRepository.save(project).getId();

        ProjectEntity otherProject = new ProjectEntity(
                "Vorheriges Projekt", 3L, 4L, "Max Mustermann", "Anderes Projektziel",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        otherProject.addEmployee(11L, "Consulting");
        otherProjectId = projectRepository.save(otherProject).getId();
    }

    @Test
    void assignmentWithUnknownEmployeeReturns404() throws Exception {
        when(employeeClient.hasQualification(999L, "Java", "test-token"))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Mitarbeiter mit der ID 999 wurde nicht gefunden."));

        Map<String, Object> request = validAssignment();
        request.put("employeeId", 999L);

        expectProblem(postRejectedAssignment(request), 404,
                "Mitarbeiter mit der ID 999 wurde nicht gefunden.");

        verify(employeeClient).hasQualification(999L, "Java", "test-token");
    }

    @Test
    void assignmentWithQualificationNotHeldByEmployeeReturns400() throws Exception {
        when(employeeClient.hasQualification(11L, "Python", "test-token"))
                .thenReturn(false);

        Map<String, Object> request = validAssignment();
        request.put("qualification", "Python");

        expectProblem(postRejectedAssignment(request), 400,
                "Mitarbeiter mit der ID 11 besitzt die Qualifikation 'Python' nicht.");

        verify(employeeClient).hasQualification(11L, "Python", "test-token");
    }

    @ParameterizedTest
    @CsvSource({
            "2026-11-01, 2026-11-10",
            "2026-11-01, 2026-11-15",
            "2026-11-12, 2026-11-18",
            "2026-11-01, 2026-11-30",
            "2026-11-15, 2026-11-30",
            "2026-11-20, 2026-11-30",
            "2026-11-10, 2026-11-20",
            "2026-11-15, 2026-11-15"
    })
    void assignmentWithOverlappingProjectReturns409(String startDate, String endDate) throws Exception {
        ProjectEntity otherProject = projectRepository.findById(otherProjectId).orElseThrow();
        otherProject.setStartDate(LocalDate.parse(startDate));
        otherProject.setEndDate(LocalDate.parse(endDate));
        projectRepository.save(otherProject);

        when(employeeClient.hasQualification(11L, "Java", "test-token"))
                .thenReturn(true);

        expectProblem(postRejectedAssignment(validAssignment()), 409,
                "Mitarbeiter mit der ID 11 ist im Zeitraum vom 2026-11-10"
                        + " bis 2026-11-20 bereits einem anderen Projekt zugeordnet.");

        verify(employeeClient).hasQualification(11L, "Java", "test-token");
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void assignmentWithNonPositiveEmployeeIdReturns400(long employeeId) throws Exception {
        Map<String, Object> request = validAssignment();
        request.put("employeeId", employeeId);

        expectValidationError(request, "employeeId");
    }

    @Test
    void assignmentWithMissingEmployeeIdReturns400() throws Exception {
        Map<String, Object> request = validAssignment();
        request.remove("employeeId");

        expectValidationError(request, "employeeId");
    }

    @Test
    void assignmentWithNullEmployeeIdReturns400() throws Exception {
        Map<String, Object> request = validAssignment();
        request.put("employeeId", null);

        expectValidationError(request, "employeeId");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "9223372036854775808"})
    void assignmentWithMalformedEmployeeIdReturns400(String employeeId) throws Exception {
        Map<String, Object> request = validAssignment();
        request.put("employeeId", employeeId);

        postRejectedAssignment(request)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").isNotEmpty());

        verifyNoInteractions(employeeClient);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", " \t "})
    void assignmentWithEmptyQualificationReturns400(String qualification) throws Exception {
        Map<String, Object> request = validAssignment();
        request.put("qualification", qualification);

        expectValidationError(request, "qualification");
    }

    @Test
    void assignmentWithTooLongQualificationReturns400() throws Exception {
        Map<String, Object> request = validAssignment();
        request.put("qualification", "a".repeat(256));

        expectValidationError(request, "qualification");
    }

    @Test
    void assignmentWithMissingQualificationReturns400() throws Exception {
        Map<String, Object> request = validAssignment();
        request.remove("qualification");

        expectValidationError(request, "qualification");
    }

    private Map<String, Object> validAssignment() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("employeeId", 11L);
        request.put("qualification", "Java");
        return request;
    }

    private ResultActions postRejectedAssignment(Map<String, Object> request) throws Exception {
        List<Map<String, Object>> projectsBefore = jdbcTemplate.queryForList(
                "SELECT * FROM projects ORDER BY id");
        List<Map<String, Object>> assignmentsBefore = jdbcTemplate.queryForList(
                "SELECT * FROM project_employees ORDER BY id");

        ResultActions response = mockMvc.perform(post("/projects/{projectId}/employees", projectId)
                        .with(jwt().jwt(builder -> builder.tokenValue("test-token")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());

        assertThat(jdbcTemplate.queryForList("SELECT * FROM projects ORDER BY id"))
                .isEqualTo(projectsBefore);
        assertThat(jdbcTemplate.queryForList("SELECT * FROM project_employees ORDER BY id"))
                .isEqualTo(assignmentsBefore);

        return response;
    }

    private ResultActions expectProblem(ResultActions response, int expectedStatus, String detail)
            throws Exception {
        return response
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.instance").value("/projects/" + projectId + "/employees"));
    }

    private void expectValidationError(Map<String, Object> request, String field) throws Exception {
        expectProblem(postRejectedAssignment(request), 400, "Die Anfrage enthält ungültige Felder.")
                .andExpect(jsonPath("$.errors." + field).isNotEmpty());

        verifyNoInteractions(employeeClient);
    }
}
