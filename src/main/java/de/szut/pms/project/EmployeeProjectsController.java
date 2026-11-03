package de.szut.pms.project;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/employees")
@Tag(name = "Projektzuordnungen", description = "Mitarbeiter und ihre Qualifikationen in Projekten")
public class EmployeeProjectsController {

    private final ProjectEmployeeService service;

    public EmployeeProjectsController(ProjectEmployeeService service) {
        this.service = service;
    }

    @Operation(summary = "Ruft die Projekte eines Mitarbeiters ab",
            description = "Prüft die Mitarbeiter-ID über den Employee-Service. Liefert pro Projekt dessen ID, "
                    + "Bezeichnung, Start- und Enddatum sowie die Qualifikation aus der jeweiligen Zuordnung. "
                    + "Sortiert nach Startdatum und Projekt-ID. Ein Mitarbeiter ohne Zuordnungen erhält eine leere Liste.")
    @ApiResponse(responseCode = "200",
            description = "Mitarbeiter-ID und Projekte mit der jeweiligen Qualifikation; die Liste kann leer sein",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = EmployeeProjectsDto.class)))
    @ApiResponse(responseCode = "400", description = "Ungültige Mitarbeiter-ID",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Ein gültiger Bearer-Token ist erforderlich",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Mitarbeiter wurde nicht gefunden",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "502", description = "Fehlerhafte Antwort des Employee-Service",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "503", description = "Employee-Service ist nicht erreichbar",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{employeeId}/projects")
    public ResponseEntity<EmployeeProjectsDto> findProjects(
            @Parameter(description = "Positive ID des Mitarbeiters im Employee-Service", example = "11")
            @PathVariable("employeeId") Long employeeId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(service.findProjectsByEmployeeId(employeeId, jwt.getTokenValue()));
    }
}
