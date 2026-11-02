package de.szut.pms.project;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import de.szut.pms.testcontainers.AbstractIntegrationTest;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectGetAllTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @BeforeEach
    void prepareProjects() {
        projectRepository.deleteAll();
    }

    @Test
    void getAllReturnsAllProjectData() throws Exception {
        ProjectEntity saved = projectRepository.save(project("Kundenportal"));

        mockMvc.perform(get("/projects").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(saved.getId()))
                .andExpect(jsonPath("$[0].name").value("Kundenportal"))
                .andExpect(jsonPath("$[0].responsibleEmployeeId").value(1))
                .andExpect(jsonPath("$[0].customerId").value(2))
                .andExpect(jsonPath("$[0].customerContactName").value("Anna Müller"))
                .andExpect(jsonPath("$[0].comment").value("Entwicklung eines Kundenportals"))
                .andExpect(jsonPath("$[0].startDate").value("2026-11-01"))
                .andExpect(jsonPath("$[0].endDate").value("2026-12-15"))
                .andExpect(jsonPath("$[0].employees").isArray())
                .andExpect(jsonPath("$[0].employees").isEmpty());
    }

    @Test
    void getAllReturnsEveryStoredProject() throws Exception {
        ProjectEntity first = projectRepository.save(project("Kundenportal"));
        ProjectEntity second = projectRepository.save(project("Interne Verwaltung"));

        mockMvc.perform(get("/projects").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id").value(containsInAnyOrder(
                        first.getId().intValue(), second.getId().intValue())))
                .andExpect(jsonPath("$[*].name").value(containsInAnyOrder(
                        "Kundenportal", "Interne Verwaltung")));
    }

    @Test
    void getAllReturnsEmployeesOfEachProject() throws Exception {
        ProjectEntity first = project("Kundenportal");
        first.addEmployee(11L, "Java");
        first.addEmployee(12L, "SQL");
        first = projectRepository.save(first);

        ProjectEntity second = project("Interne Verwaltung");
        second.addEmployee(21L, "Consulting");
        second = projectRepository.save(second);

        String firstEmployees = "$[?(@.id == " + first.getId() + ")].employees";
        String secondEmployees = "$[?(@.id == " + second.getId() + ")].employees";

        mockMvc.perform(get("/projects").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath(firstEmployees + "[*].employeeId")
                        .value(containsInAnyOrder(11, 12)))
                .andExpect(jsonPath(firstEmployees + "[*].qualification")
                        .value(containsInAnyOrder("Java", "SQL")))
                .andExpect(jsonPath(secondEmployees + "[*].employeeId").value(contains(21)))
                .andExpect(jsonPath(secondEmployees + "[*].qualification").value(contains("Consulting")));
    }

    @Test
    void getAllReturnsEmptyListWhenNoProjectsExist() throws Exception {
        mockMvc.perform(get("/projects").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getAllWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/projects"))
                .andExpect(status().isUnauthorized());
    }

    private ProjectEntity project(String name) {
        return new ProjectEntity(
                name, 1L, 2L, "Anna Müller", "Entwicklung eines Kundenportals",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 15));
    }
}
