package de.szut.pms.project;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import tools.jackson.databind.ObjectMapper;

import de.szut.pms.customer.CustomerClient;
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

class ProjectCreationTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeClient employeeClient;

    @MockitoBean
    private CustomerClient customerClient;

    @BeforeEach
    void prepareProjects() {
        projectRepository.deleteAll();

        when(employeeClient.existsById(1L, "test-token"))
                .thenReturn(true);

        when(customerClient.existsById(1L))
                .thenReturn(true);
    }

    @Test
    void createStoresAndReturnsProject() throws Exception {
        ResultActions response = postProject(validProject())
                .andExpect(status().isCreated());

        List<ProjectEntity> projects = projectRepository.findAll();
        assertThat(projects).hasSize(1);

        ProjectEntity saved = projects.getFirst();

        assertThat(saved.getId()).isPositive();
        assertThat(saved.getName()).isEqualTo("Kundenportal");
        assertThat(saved.getResponsibleEmployeeId()).isEqualTo(1L);
        assertThat(saved.getCustomerId()).isEqualTo(1L);
        assertThat(saved.getCustomerContactName()).isEqualTo("Anna Müller");
        assertThat(saved.getComment())
                .isEqualTo("Entwicklung eines Kundenportals");
        assertThat(saved.getStartDate())
                .isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(saved.getEndDate())
                .isEqualTo(LocalDate.of(2026, 12, 15));

        response
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("Kundenportal"))
                .andExpect(jsonPath("$.responsibleEmployeeId").value(1))
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.customerContactName").value("Anna Müller"))
                .andExpect(jsonPath("$.comment")
                        .value("Entwicklung eines Kundenportals"))
                .andExpect(jsonPath("$.startDate").value("2026-11-01"))
                .andExpect(jsonPath("$.endDate").value("2026-12-15"))
                .andExpect(jsonPath("$.employees").isArray())
                .andExpect(jsonPath("$.employees").isEmpty());

        verify(employeeClient).existsById(1L, "test-token");
        verify(customerClient).existsById(1L);
    }

    @Test
    void createAssignsDifferentIdsToProjects() throws Exception {
        postProject(validProject()).andExpect(status().isCreated());
        postProject(validProject()).andExpect(status().isCreated());

        List<ProjectEntity> projects = projectRepository.findAll();
        assertThat(projects).hasSize(2);
        assertThat(projects.getFirst().getId()).isPositive();
        assertThat(projects.get(1).getId())
                .isPositive()
                .isNotEqualTo(projects.getFirst().getId());
    }

    @Test
    void createWithUnknownEmployeeIsRejected() throws Exception {
        when(employeeClient.existsById(999L, "test-token"))
                .thenReturn(false);

        Map<String, Object> project = validProject();
        project.put("responsibleEmployeeId", 999L);

        postProject(project)
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail")
                        .value("Mitarbeiter mit der ID 999 wurde nicht gefunden."));

        assertThat(projectRepository.count()).isZero();
        verify(employeeClient).existsById(999L, "test-token");
    }

    @Test
    void createWithUnknownCustomerIsRejected() throws Exception {
        when(customerClient.existsById(999L)).thenReturn(false);

        Map<String, Object> project = validProject();
        project.put("customerId", 999L);

        postProject(project)
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail")
                        .value("Kunde mit der ID 999 wurde nicht gefunden."));

        assertThat(projectRepository.count()).isZero();
        verify(employeeClient).existsById(1L, "test-token");
        verify(customerClient).existsById(999L);
    }

    @Test
    void createWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validProject())))
                .andExpect(status().isUnauthorized());

        assertThat(projectRepository.count()).isZero();
        verifyNoInteractions(employeeClient, customerClient);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "name", "responsibleEmployeeId", "customerId", "customerContactName",
            "comment", "startDate", "endDate"
    })
    void createWithMissingRequiredFieldIsRejected(String field) throws Exception {
        Map<String, Object> project = validProject();
        project.remove(field);

        assertValidationError(project, field);
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "customerContactName", "comment"})
    void createWithBlankTextIsRejected(String field) throws Exception {
        Map<String, Object> project = validProject();
        project.put(field, " ");

        assertValidationError(project, field);
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "customerContactName"})
    void createWithTooLongTextIsRejected(String field) throws Exception {
        Map<String, Object> project = validProject();
        project.put(field, "a".repeat(256));

        assertValidationError(project, field);
    }

    @ParameterizedTest
    @CsvSource({
            "responsibleEmployeeId, 0",
            "responsibleEmployeeId, -1",
            "customerId, 0",
            "customerId, -1"
    })
    void createWithNonPositiveIdIsRejected(String field, long id) throws Exception {
        Map<String, Object> project = validProject();
        project.put(field, id);

        assertValidationError(project, field);
    }

    @Test
    void createWithEndDateBeforeStartDateIsRejected() throws Exception {
        Map<String, Object> project = validProject();
        project.put("endDate", "2026-10-31");

        assertValidationError(project, "dateRangeValid");
    }

    @Test
    void createWithEqualStartAndEndDatesIsAccepted() throws Exception {
        Map<String, Object> project = validProject();
        project.put("endDate", "2026-11-01");

        postProject(project)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startDate").value("2026-11-01"))
                .andExpect(jsonPath("$.endDate").value("2026-11-01"));

        List<ProjectEntity> projects = projectRepository.findAll();
        assertThat(projects).hasSize(1);
        assertThat(projects.getFirst().getStartDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(projects.getFirst().getEndDate()).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    private Map<String, Object> validProject() {
        Map<String, Object> project = new LinkedHashMap<>();
        project.put("name", "Kundenportal");
        project.put("responsibleEmployeeId", 1L);
        project.put("customerId", 1L);
        project.put("customerContactName", "Anna Müller");
        project.put("comment", "Entwicklung eines Kundenportals");
        project.put("startDate", "2026-11-01");
        project.put("endDate", "2026-12-15");
        return project;
    }

    private ResultActions postProject(Map<String, Object> project) throws Exception {
        return mockMvc.perform(post("/projects")
                .with(jwt().jwt(builder -> builder.tokenValue("test-token")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(project)));
    }

    private void assertValidationError(Map<String, Object> project, String field) throws Exception {
        postProject(project)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors." + field).exists());

        assertThat(projectRepository.count()).isZero();
        verifyNoInteractions(employeeClient, customerClient);
    }
}
