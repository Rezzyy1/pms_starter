package de.szut.pms.project;

import java.util.List;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/projects")
@Tag(name = "Projekte", description = "Verwaltung von Kundenprojekten")
public class ProjectReadController {

    private final ProjectReadService service;

    public ProjectReadController(ProjectReadService service) {
        this.service = service;
    }

    @Operation(summary = "Ruft alle Projekte ab",
            description = "Liefert die vollständigen Projektdaten einschließlich der zugeordneten Mitarbeiter.")
    @ApiResponse(responseCode = "200",
            description = "Projektliste wurde abgerufen; ohne gespeicherte Projekte wird [] zurückgegeben",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ProjectDto.class))))
    @ApiResponse(responseCode = "400", description = "Ungültige Bearer-Authentifizierungsanfrage (invalid_request)",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Ein gültiger Bearer-Token ist erforderlich",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    public ResponseEntity<List<ProjectDto>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @Operation(summary = "Ruft ein Projekt anhand seiner ID ab")
    @ApiResponse(responseCode = "200", description = "Projekt wurde gefunden",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ProjectDto.class)))
    @ApiResponse(responseCode = "400", description = "Ungültige Projekt-ID",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Ein gültiger Bearer-Token ist erforderlich",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Projekt wurde nicht gefunden",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> findById(
            @Parameter(description = "ID des Projekts", example = "1") @PathVariable("id") Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @Operation(summary = "Ruft die Mitarbeiter eines Projekts ab")
    @ApiResponse(responseCode = "200",
            description = "Projekt-ID, Projektbezeichnung und Mitarbeiter; die Mitarbeiterliste kann leer sein",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ProjectEmployeesDto.class)))
    @ApiResponse(responseCode = "400", description = "Ungültige Projekt-ID",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Ein gültiger Bearer-Token ist erforderlich",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Projekt wurde nicht gefunden",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{projectId}/employees")
    public ResponseEntity<ProjectEmployeesDto> findEmployees(
            @Parameter(description = "ID des Projekts", example = "1")
            @PathVariable("projectId") Long projectId) {
        return ResponseEntity.ok(service.findEmployeesByProjectId(projectId));
    }
}
