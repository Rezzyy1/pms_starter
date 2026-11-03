package de.szut.pms.project;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/projects/{projectId}/employees")
public class ProjectEmployeeController {

    private final ProjectEmployeeService service;

    public ProjectEmployeeController(ProjectEmployeeService service) {
        this.service = service;
    }

    @Operation(summary = "Fügt einem bestehenden Projekt einen Mitarbeiter hinzu")
    @ApiResponse(responseCode = "201", description = "Mitarbeiter wurde dem Projekt zugeordnet")
    @ApiResponse(responseCode = "400",
            description = "Ungültige Projekt-ID, Eingabefelder oder Qualifikation des Mitarbeiters",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Authentifizierung ist erforderlich",
            content = @Content)
    @ApiResponse(responseCode = "404", description = "Projekt oder Mitarbeiter wurde nicht gefunden",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409",
            description = "Mitarbeiter ist bereits zugeordnet oder in einem anderen Projekt zeitlich gebunden",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "502", description = "Fehlerhafte Antwort des Employee-Service",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "503", description = "Employee-Service ist nicht erreichbar",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    public ResponseEntity<ProjectEmployeeDto> addEmployee(
            @PathVariable("projectId") Long projectId,
            @RequestBody @Valid AddProjectEmployeeDto dto,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        ProjectEmployeeDto created = service.addEmployee(projectId, dto, jwt.getTokenValue());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Entfernt einen Mitarbeiter aus einem Projekt")
    @ApiResponse(responseCode = "200",
            description = "Zuordnung wurde gelöscht; Mitarbeiter-ID und Qualifikation werden zurückgegeben")
    @ApiResponse(responseCode = "400", description = "Ungültige Projekt- oder Mitarbeiter-ID",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Authentifizierung ist erforderlich",
            content = @Content)
    @ApiResponse(responseCode = "404",
            description = "Projekt wurde nicht gefunden oder Mitarbeiter ist dem Projekt nicht zugeordnet",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{employeeId}")
    public ResponseEntity<ProjectEmployeeDto> removeEmployee(
            @PathVariable("projectId") Long projectId,
            @PathVariable("employeeId") Long employeeId) {

        return ResponseEntity.ok(service.removeEmployee(projectId, employeeId));
    }
}
