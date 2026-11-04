package de.szut.pms.project;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import de.szut.pms.employee.EmployeeClient;
import de.szut.pms.testcontainers.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectEmployeeConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private EmployeeClient employeeClient;

    @BeforeEach
    void prepareProjects() {
        projectRepository.deleteAll();
        when(employeeClient.hasQualification(11L, "Java", "test-token")).thenReturn(true);
    }

    @Test
    void concurrentAssignmentsToOverlappingProjectsReturn201And409() throws Exception {
        Long firstId = projectRepository.save(project("2026-11-10", "2026-11-20")).getId();
        Long secondId = projectRepository.save(project("2026-11-15", "2026-11-25")).getId();

        expectConflictAfterCommit(assignment(firstId), 201, assignment(secondId));

        assertThat(projectRepository.findById(firstId).orElseThrow().getEmployees()).hasSize(1);
        assertThat(projectRepository.findById(secondId).orElseThrow().getEmployees()).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM project_employees WHERE employee_id = 11", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void concurrentDuplicateAssignmentsReturn201And409() throws Exception {
        Long projectId = projectRepository.save(project("2026-11-10", "2026-11-20")).getId();

        MvcResult conflict = expectConflictAfterCommit(assignment(projectId), 201, assignment(projectId));

        jsonPath("$.detail").value(
                "Mitarbeiter mit der ID 11 ist diesem Projekt bereits zugeordnet.").match(conflict);
        assertThat(projectRepository.findById(projectId).orElseThrow().getEmployees()).hasSize(1);
    }

    @Test
    void concurrentDateChangesCannotCreateAnOverlap() throws Exception {
        ProjectEntity first = project("2026-11-01", "2026-11-10");
        first.addEmployee(11L, "Java");
        Long firstId = projectRepository.save(first).getId();
        ProjectEntity second = project("2026-11-20", "2026-11-30");
        second.addEmployee(11L, "Java");
        Long secondId = projectRepository.save(second).getId();

        expectConflictAfterCommit(
                dateChange(firstId, "2026-11-01", "2026-11-18"), 204,
                dateChange(secondId, "2026-11-15", "2026-11-30"));

        assertThat(projectRepository.findById(firstId).orElseThrow().getEndDate())
                .isEqualTo(LocalDate.of(2026, 11, 18));
        assertThat(projectRepository.findById(secondId).orElseThrow().getStartDate())
                .isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @Test
    void dateChangeWaitsForAConcurrentAssignment() throws Exception {
        Long firstId = projectRepository.save(project("2026-11-01", "2026-11-10")).getId();
        ProjectEntity second = project("2026-11-20", "2026-11-30");
        second.addEmployee(11L, "Java");
        Long secondId = projectRepository.save(second).getId();

        expectConflictAfterCommit(assignment(firstId), 201,
                dateChange(secondId, "2026-11-05", "2026-11-30"));

        assertThat(projectRepository.findById(firstId).orElseThrow().getEmployees()).hasSize(1);
        assertThat(projectRepository.findById(secondId).orElseThrow().getStartDate())
                .isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @Test
    void assignmentWaitsForAConcurrentDateChange() throws Exception {
        Long firstId = projectRepository.save(project("2026-11-01", "2026-11-10")).getId();
        ProjectEntity second = project("2026-11-20", "2026-11-30");
        second.addEmployee(11L, "Java");
        Long secondId = projectRepository.save(second).getId();

        expectConflictAfterCommit(dateChange(secondId, "2026-11-05", "2026-11-30"), 204,
                assignment(firstId));

        assertThat(projectRepository.findById(firstId).orElseThrow().getEmployees()).isEmpty();
        assertThat(projectRepository.findById(secondId).orElseThrow().getStartDate())
                .isEqualTo(LocalDate.of(2026, 11, 5));
    }

    private MvcResult expectConflictAfterCommit(MockHttpServletRequestBuilder firstRequest,
            int firstStatus, MockHttpServletRequestBuilder secondRequest) throws Exception {
        CountDownLatch firstResponseReady = new CountDownLatch(1);
        CountDownLatch allowFirstCommit = new CountDownLatch(1);
        AtomicInteger secondConnectionId = new AtomicInteger();

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> new TransactionTemplate(transactionManager).execute(transaction -> {
                try {
                    MvcResult result = perform(firstRequest);
                    firstResponseReady.countDown();
                    if (!allowFirstCommit.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Freigabe des ersten Commits fehlt.");
                    }
                    return result;
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                } finally {
                    firstResponseReady.countDown();
                }
            }));

            try {
                assertThat(firstResponseReady.await(30, TimeUnit.SECONDS)).isTrue();
                var second = executor.submit(() -> new TransactionTemplate(transactionManager).execute(transaction -> {
                    secondConnectionId.set(jdbcTemplate.queryForObject("SELECT pg_backend_pid()", Integer.class));
                    MvcResult result = perform(secondRequest);
                    if (result.getResponse().getStatus() >= 400) {
                        transaction.setRollbackOnly();
                    }
                    return result;
                }));

                try {
                    // Der erste Request ist bearbeitet, aber noch nicht committed.
                    // Der zweite muss in PostgreSQL warten, statt veraltete Daten zu prüfen.
                    await().atMost(Duration.ofSeconds(10)).until(() ->
                            second.isDone() || isWaitingForDatabaseLock(secondConnectionId.get()));
                    assertThat(second.isDone()).as("Zweiter Request wartet auf den ersten Commit").isFalse();
                } finally {
                    allowFirstCommit.countDown();
                }

                status().is(firstStatus).match(first.get(30, TimeUnit.SECONDS));
                MvcResult conflict = second.get(30, TimeUnit.SECONDS);
                status().isConflict().match(conflict);
                content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON).match(conflict);
                jsonPath("$.status").value(409).match(conflict);
                jsonPath("$.detail").isNotEmpty().match(conflict);
                return conflict;
            } finally {
                allowFirstCommit.countDown();
            }
        }
    }

    private boolean isWaitingForDatabaseLock(int connectionId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM pg_stat_activity
                    WHERE pid = ? AND wait_event_type = 'Lock'
                )
                """, Boolean.class, connectionId));
    }

    private MvcResult perform(MockHttpServletRequestBuilder request) {
        try {
            return mockMvc.perform(request.with(jwt().jwt(builder -> builder.tokenValue("test-token"))))
                    .andReturn();
        } catch (Exception exception) {
            throw new IllegalStateException("Testanfrage konnte nicht ausgeführt werden.", exception);
        }
    }

    private MockHttpServletRequestBuilder assignment(Long projectId) {
        return post("/projects/{projectId}/employees", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"employeeId": 11, "qualification": "Java"}
                        """);
    }

    private MockHttpServletRequestBuilder dateChange(Long projectId, String startDate, String endDate) {
        return put("/projects/{id}", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Kundenportal",
                          "responsibleEmployeeId": 1,
                          "customerId": 2,
                          "customerContactName": "Anna Müller",
                          "comment": "Projektziel",
                          "startDate": "%s",
                          "endDate": "%s"
                        }
                        """.formatted(startDate, endDate));
    }

    private ProjectEntity project(String startDate, String endDate) {
        return new ProjectEntity("Kundenportal", 1L, 2L, "Anna Müller", "Projektziel",
                LocalDate.parse(startDate), LocalDate.parse(endDate));
    }
}
