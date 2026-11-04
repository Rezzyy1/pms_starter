package de.szut.pms.project;

import java.time.LocalDate;
import java.util.LinkedHashMap;
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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectModificationInvalidIdTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeClient employeeClient;

    @MockitoBean
    private CustomerClient customerClient;

    private Long projectId;

    @BeforeEach
    void prepareProjects() {
        projectRepository.deleteAll();

        ProjectEntity project = new ProjectEntity(
                "Kundenportal", 1L, 2L, "Anna Müller", "Bestehendes Projektziel",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 15));
        project.addEmployee(11L, "Java");
        projectId = projectRepository.save(project).getId();
    }

    @Test
    void updateWithUnknownEmployeeIsRejectedWithoutSavingChanges() throws Exception {
        when(employeeClient.existsById(99L, "test-token")).thenReturn(false);

        expectBadRequest(updateProject(projectId, validUpdate()))
                .andExpect(jsonPath("$.detail")
                        .value("Mitarbeiter mit der ID 99 existiert nicht."));

        assertStoredProjectUnchanged();
        verify(employeeClient).existsById(99L, "test-token");
        verifyNoInteractions(customerClient);
    }

    @Test
    void updateWithUnknownCustomerIsRejectedWithoutSavingChanges() throws Exception {
        when(employeeClient.existsById(99L, "test-token")).thenReturn(true);
        when(customerClient.existsById(88L)).thenReturn(false);

        expectBadRequest(updateProject(projectId, validUpdate()))
                .andExpect(jsonPath("$.detail")
                        .value("Kunde mit der ID 88 existiert nicht."));

        assertStoredProjectUnchanged();
        verify(employeeClient).existsById(99L, "test-token");
        verify(customerClient).existsById(88L);
    }

    @ParameterizedTest
    @CsvSource({
            "responsibleEmployeeId, 0",
            "responsibleEmployeeId, -1",
            "customerId, 0",
            "customerId, -1"
    })
    void updateWithNonPositiveIdIsRejected(String field, long id) throws Exception {
        Map<String, Object> request = validUpdate();
        request.put(field, id);

        assertValidationError(request, field);
    }

    @ParameterizedTest
    @ValueSource(strings = {"responsibleEmployeeId", "customerId"})
    void updateWithMissingIdIsRejected(String field) throws Exception {
        Map<String, Object> request = validUpdate();
        request.remove(field);

        assertValidationError(request, field);
    }

    @ParameterizedTest
    @ValueSource(strings = {"responsibleEmployeeId", "customerId"})
    void updateWithNullIdIsRejected(String field) throws Exception {
        Map<String, Object> request = validUpdate();
        request.put(field, null);

        assertValidationError(request, field);
    }

    @ParameterizedTest
    @CsvSource({
            "responsibleEmployeeId, abc",
            "responsibleEmployeeId, 9223372036854775808",
            "customerId, abc",
            "customerId, 9223372036854775808"
    })
    void updateWithMalformedReferenceIdIsRejected(String field, String id) throws Exception {
        Map<String, Object> request = validUpdate();
        request.put(field, id);

        expectBadRequest(updateProject(projectId, request));

        assertStoredProjectUnchanged();
        verifyNoInteractions(employeeClient, customerClient);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "9223372036854775808"})
    void updateWithMalformedProjectIdIsRejected(String id) throws Exception {
        expectBadRequest(updateProject(id, validUpdate()));

        assertStoredProjectUnchanged();
        verifyNoInteractions(employeeClient, customerClient);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "9223372036854775808"})
    void deleteWithMalformedProjectIdIsRejected(String id) throws Exception {
        expectBadRequest(mockMvc.perform(delete("/projects/{id}", id).with(jwt())));

        assertStoredProjectUnchanged();
        verifyNoInteractions(employeeClient, customerClient);
    }

    private Map<String, Object> validUpdate() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", "Neues Kundenportal");
        request.put("responsibleEmployeeId", 99L);
        request.put("customerId", 88L);
        request.put("customerContactName", "Maria Beispiel");
        request.put("comment", "Neues Projektziel");
        request.put("startDate", "2026-11-10");
        request.put("endDate", "2026-12-31");
        return request;
    }

    private ResultActions updateProject(Object id, Map<String, Object> request) throws Exception {
        return mockMvc.perform(put("/projects/{id}", id)
                .with(jwt().jwt(builder -> builder.tokenValue("test-token")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions expectBadRequest(ResultActions response) throws Exception {
        return response
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    private void assertValidationError(Map<String, Object> request, String field) throws Exception {
        expectBadRequest(updateProject(projectId, request))
                .andExpect(jsonPath("$.errors." + field).isNotEmpty());

        assertStoredProjectUnchanged();
        verifyNoInteractions(employeeClient, customerClient);
    }

    private void assertStoredProjectUnchanged() {
        assertThat(projectRepository.count()).isEqualTo(1);

        ProjectEntity stored = projectRepository.findById(projectId).orElseThrow();
        assertThat(stored.getId()).isEqualTo(projectId);
        assertThat(stored.getName()).isEqualTo("Kundenportal");
        assertThat(stored.getResponsibleEmployeeId()).isEqualTo(1L);
        assertThat(stored.getCustomerId()).isEqualTo(2L);
        assertThat(stored.getCustomerContactName()).isEqualTo("Anna Müller");
        assertThat(stored.getComment()).isEqualTo("Bestehendes Projektziel");
        assertThat(stored.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(stored.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 15));
        assertThat(stored.getEmployees())
                .extracting(
                        ProjectEmployeeEntity::getEmployeeId,
                        ProjectEmployeeEntity::getQualification)
                .containsExactly(tuple(11L, "Java"));
    }
}
