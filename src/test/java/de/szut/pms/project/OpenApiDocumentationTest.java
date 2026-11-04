package de.szut.pms.project;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import de.szut.pms.testcontainers.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiDocumentationTest extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void swaggerUiIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("swagger-ui-bundle.js")));
    }

    @Test
    void swaggerConfigurationUsesTheLocalOpenApiDocument() throws Exception {
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/v3/api-docs"))
                .andExpect(jsonPath("$.tryItOutEnabled").value(true));
    }

    @Test
    void allProjectOperationsHaveResponsesAndBearerAuthentication() throws Exception {
        var document = openApiDocument();

        assertThat(document.at("/servers/0/url").asText()).isEqualTo("/");
        assertThat(document.at("/components/securitySchemes/bearerAuth/type").asText())
                .isEqualTo("http");
        assertThat(document.at("/components/securitySchemes/bearerAuth/scheme").asText())
                .isEqualTo("bearer");
        assertThat(document.at("/components/securitySchemes/bearerAuth/bearerFormat").asText())
                .isEqualTo("JWT");
        assertThat(document.at("/security/0/bearerAuth").isArray()).isTrue();

        var expectedOperations = List.of(
                new DocumentedOperation("/projects", "get", "200"),
                new DocumentedOperation("/projects", "post", "201"),
                new DocumentedOperation("/projects/{id}", "get", "200"),
                new DocumentedOperation("/projects/{id}", "put", "204"),
                new DocumentedOperation("/projects/{id}", "delete", "204"),
                new DocumentedOperation("/projects/{projectId}/employees", "get", "200"),
                new DocumentedOperation("/projects/{projectId}/employees", "post", "201"),
                new DocumentedOperation("/projects/{projectId}/employees/{employeeId}", "delete", "200"),
                new DocumentedOperation("/employees/{employeeId}/projects", "get", "200"));

        assertThat(document.path("paths").size()).isEqualTo(5);
        for (var expected : expectedOperations) {
            var operation = document.path("paths").path(expected.path()).path(expected.method());
            assertThat(operation.isMissingNode()).as(expected.method() + " " + expected.path()).isFalse();
            assertThat(operation.path("responses").has(expected.successCode())).isTrue();
            assertThat(operation.path("responses").has("401")).isTrue();
            var security = operation.has("security") ? operation.path("security") : document.path("security");
            assertThat(security.path(0).has("bearerAuth")).isTrue();

            for (var parameter : operation.path("parameters")) {
                assertThat(parameter.path("in").asText()).isEqualTo("path");
                assertThat(parameter.path("required").asBoolean()).isTrue();
                assertThat(parameter.path("schema").path("type").asText()).isEqualTo("integer");
            }
        }
    }

    @Test
    void writeOperationsExposeRequiredJsonRequestBodies() throws Exception {
        var paths = openApiDocument().path("paths");

        assertRequestBody(paths.path("/projects").path("post"), "CreateProjectDto");
        assertRequestBody(paths.path("/projects/{id}").path("put"), "UpdateProjectDto");
        assertRequestBody(paths.path("/projects/{projectId}/employees").path("post"),
                "AddProjectEmployeeDto");
    }

    @Test
    void errorSchemaDescribesTheActualValidationResponse() throws Exception {
        String response = mockMvc.perform(post("/projects").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "responsibleEmployeeId": 1,
                                  "customerId": 2,
                                  "customerContactName": "Anna Müller",
                                  "comment": "Projektziel",
                                  "startDate": "2026-11-01",
                                  "endDate": "2026-12-15"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.name").value("Die Projektbezeichnung ist erforderlich."))
                .andReturn().getResponse().getContentAsString();

        JsonNode problem = objectMapper.readTree(response);
        JsonNode schema = openApiDocument().at("/components/schemas/ProblemDetail");
        assertDocumentedProblemFields(schema, problem);
        assertThat(schema.at("/properties/errors/type").asText()).isEqualTo("object");
        assertThat(schema.at("/properties/errors/additionalProperties/type").asText()).isEqualTo("string");
        assertThat(schema.path("properties").has("properties")).isFalse();
        assertThat(problem.has("properties")).isFalse();
    }

    @Test
    void errorSchemaAllowsAuthenticationErrorsWithoutTypeAndFieldErrors() throws Exception {
        String response = mockMvc.perform(get("/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn().getResponse().getContentAsString();

        JsonNode problem = objectMapper.readTree(response);
        JsonNode schema = openApiDocument().at("/components/schemas/ProblemDetail");
        assertDocumentedProblemFields(schema, problem);
        assertThat(problem.has("type")).isFalse();
        assertThat(problem.has("errors")).isFalse();
        assertThat(schema.path("properties").has("type")).isTrue();
        assertThat(schema.path("properties").has("errors")).isTrue();
        for (JsonNode required : schema.path("required")) {
            assertThat(required.asText()).isNotIn("type", "errors");
        }
    }

    private void assertDocumentedProblemFields(JsonNode schema, JsonNode problem) {
        for (String field : problem.propertyNames()) {
            assertThat(schema.path("properties").has(field))
                    .as("Dokumentiertes Antwortfeld: %s", field).isTrue();
        }
        for (JsonNode required : schema.path("required")) {
            assertThat(problem.has(required.asText()))
                    .as("Pflichtfeld in der Antwort: %s", required.asText()).isTrue();
        }
    }

    private JsonNode openApiDocument() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(json);
    }

    private void assertRequestBody(JsonNode operation, String schemaName) {
        var body = operation.path("requestBody");
        assertThat(body.path("required").asBoolean()).isTrue();
        assertThat(body.path("content").path("application/json").path("schema").path("$ref").asText())
                .isEqualTo("#/components/schemas/" + schemaName);
    }

    private record DocumentedOperation(String path, String method, String successCode) {
    }
}
