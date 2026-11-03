package de.szut.pms.project;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.szut.pms.customer.CustomerClient;
import de.szut.pms.employee.EmployeeClient;
import de.szut.pms.testcontainers.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectPermissionsTest extends AbstractIntegrationTest {

    private static final String PROJECT_REQUEST = """
            {
              "name": "Kundenportal",
              "responsibleEmployeeId": 1,
              "customerId": 1,
              "customerContactName": "Anna Müller",
              "comment": "Entwicklung eines Kundenportals",
              "startDate": "2026-11-01",
              "endDate": "2026-12-15"
            }
            """;

    @Autowired
    private ProjectRepository projectRepository;

    @MockitoBean
    private EmployeeClient employeeClient;

    @MockitoBean
    private CustomerClient customerClient;

    @BeforeEach
    void prepareProjects() {
        projectRepository.deleteAll();
        when(employeeClient.existsById(1L, "test-token")).thenReturn(true);
        when(customerClient.existsById(1L)).thenReturn(true);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("authenticatedUsers")
    void allAuthenticatedUsersHaveTheSameProjectPermissions(
            String username, List<GrantedAuthority> authorities) throws Exception {

        var user = jwt()
                .jwt(builder -> builder.tokenValue("test-token").subject(username))
                .authorities(authorities);

        mockMvc.perform(post("/projects").with(user)
                        .contentType(MediaType.APPLICATION_JSON).content(PROJECT_REQUEST))
                .andExpect(status().isCreated());
        Long projectId = projectRepository.findAll().getFirst().getId();

        mockMvc.perform(get("/projects").with(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/projects/{id}", projectId).with(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kundenportal"));
        mockMvc.perform(get("/projects/{id}/employees", projectId).with(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId));

        mockMvc.perform(put("/projects/{id}", projectId).with(user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROJECT_REQUEST.replace("Kundenportal", "Kundenportal aktualisiert")))
                .andExpect(status().isNoContent());
        assertThat(projectRepository.findById(projectId).orElseThrow().getName())
                .isEqualTo("Kundenportal aktualisiert");

        mockMvc.perform(delete("/projects/{id}", projectId).with(user))
                .andExpect(status().isNoContent());
        assertThat(projectRepository.count()).isZero();
    }

    private static Stream<Arguments> authenticatedUsers() {
        return Stream.of(
                Arguments.of("user-without-roles", List.of()),
                Arguments.of("employee", List.of(
                        new SimpleGrantedAuthority("ROLE_EMPLOYEE"),
                        new SimpleGrantedAuthority("SCOPE_read"))),
                Arguments.of("admin", List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("SCOPE_write"))));
    }
}
