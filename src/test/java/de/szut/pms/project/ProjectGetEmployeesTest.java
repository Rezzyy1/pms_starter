package de.szut.pms.project;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import de.szut.pms.testcontainers.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectGetEmployeesTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @BeforeEach
    void prepareProjects() {
        projectRepository.deleteAll();
    }

    @Test
    void getEmployeesReturnsOnlyEmployeesOfTheRequestedProject() throws Exception {
        ProjectEntity otherProject = project("Interne Verwaltung");
        otherProject.addEmployee(21L, "Consulting");
        projectRepository.save(otherProject);

        ProjectEntity project = project("Kundenportal");
        project.addEmployee(11L, "Java");
        project.addEmployee(12L, "SQL");
        project = projectRepository.save(project);

        mockMvc.perform(get("/projects/{id}/employees", project.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.projectId").value(project.getId()))
                .andExpect(jsonPath("$.projectName").value("Kundenportal"))
                .andExpect(jsonPath("$.employees.length()").value(2))
                .andExpect(jsonPath("$.employees[*].employeeId").value(containsInAnyOrder(11, 12)))
                .andExpect(jsonPath("$.employees[?(@.employeeId == 11)].qualification").value(contains("Java")))
                .andExpect(jsonPath("$.employees[?(@.employeeId == 12)].qualification").value(contains("SQL")));

        assertThat(projectRepository.count()).isEqualTo(2);
    }

    @Test
    void getEmployeesWithoutAuthenticationIsRejected() throws Exception {
        ProjectEntity project = projectRepository.save(project("Kundenportal"));

        mockMvc.perform(get("/projects/{id}/employees", project.getId()))
                .andExpect(status().isUnauthorized());

        assertThat(projectRepository.count()).isEqualTo(1);
    }

    @Test
    void getEmployeesWithInvalidProjectIdIsRejected() throws Exception {
        mockMvc.perform(get("/projects/abc/employees").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));

        assertThat(projectRepository.count()).isZero();
    }

    private ProjectEntity project(String name) {
        return new ProjectEntity(
                name, 1L, 2L, "Anna Müller", "Entwicklung eines Kundenportals",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 15));
    }
}
