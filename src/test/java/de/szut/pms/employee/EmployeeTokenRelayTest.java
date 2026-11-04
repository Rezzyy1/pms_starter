package de.szut.pms.employee;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import de.szut.pms.project.ProjectRepository;
import de.szut.pms.testcontainers.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EmployeeTokenRelayTest extends AbstractIntegrationTest {

    private static final ConcurrentLinkedQueue<EmployeeRequest> employeeRequests = new ConcurrentLinkedQueue<>();
    private static final HttpServer employeeServer = startEmployeeServer();

    @Autowired
    private ProjectRepository projectRepository;

    @DynamicPropertySource
    static void employeeServiceProperties(DynamicPropertyRegistry registry) {
        registry.add("employee-service.base-url", () ->
                "http://127.0.0.1:" + employeeServer.getAddress().getPort());
    }

    @BeforeEach
    void prepareProjects() {
        projectRepository.deleteAll();
        employeeRequests.clear();
    }

    @AfterAll
    static void stopEmployeeServer() {
        employeeServer.stop(0);
    }

    @Test
    void projectCreationRelaysTheCallersToken() throws Exception {
        createProject("creation-token");

        assertThat(employeeRequests).containsExactly(
                new EmployeeRequest("GET", "/employees/1", "Bearer creation-token"));
    }

    @Test
    void employeeChangeRelaysTheCurrentRequestsToken() throws Exception {
        Long projectId = createProject("original-token");

        mockMvc.perform(put("/projects/{id}", projectId)
                        .with(jwt().jwt(builder -> builder.tokenValue("current-token")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectRequest(2L)))
                .andExpect(status().isNoContent());

        assertThat(employeeRequests).containsExactly(
                new EmployeeRequest("GET", "/employees/1", "Bearer original-token"),
                new EmployeeRequest("GET", "/employees/2", "Bearer current-token"));
        assertThat(projectRepository.findById(projectId).orElseThrow().getResponsibleEmployeeId())
                .isEqualTo(2L);
    }

    private Long createProject(String token) throws Exception {
        mockMvc.perform(post("/projects")
                        .with(jwt().jwt(builder -> builder.tokenValue(token)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectRequest(1L)))
                .andExpect(status().isCreated());
        return projectRepository.findAll().getFirst().getId();
    }

    private String projectRequest(long employeeId) {
        return """
                {
                  "name": "Kundenportal",
                  "responsibleEmployeeId": %d,
                  "customerId": 1,
                  "customerContactName": "Anna Müller",
                  "comment": "Entwicklung eines Kundenportals",
                  "startDate": "2026-11-01",
                  "endDate": "2026-12-15"
                }
                """.formatted(employeeId);
    }

    private static HttpServer startEmployeeServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/employees/", exchange -> {
                try (exchange) {
                    employeeRequests.add(new EmployeeRequest(
                            exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                            exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION)));
                    exchange.sendResponseHeaders(200, -1);
                }
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException("Employee-Testserver konnte nicht gestartet werden.", exception);
        }
    }

    private record EmployeeRequest(String method, String path, String authorization) {
    }
}
