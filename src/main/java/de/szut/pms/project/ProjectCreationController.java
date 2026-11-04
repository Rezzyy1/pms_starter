package de.szut.pms.project;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/projects")
@Tag(name = "Projekte", description = "Verwaltung von Kundenprojekten")
public class ProjectCreationController {

    private final ProjectCreationService service;

    public ProjectCreationController(ProjectCreationService service) {
        this.service = service;
    }

    @Operation(summary = "Legt ein neues Kundenprojekt an",
            description = "Übernimmt die Projektdaten als JSON. Die Projekt-ID wird automatisch vergeben. "
                    + "Der verantwortliche Mitarbeiter und der Kunde werden vor dem Speichern geprüft.")
    @ApiResponse(responseCode = "201", description = "Gespeichertes Projekt mit seiner erzeugten ID",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ProjectDto.class)))
    @ApiResponse(responseCode = "400", description = "Ungültiges JSON, Eingabefelder oder Projektzeitraum",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Ein gültiger Bearer-Token ist erforderlich",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Mitarbeiter oder Kunde wurde nicht gefunden",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "502", description = "Fehlerhafte Antwort des Employee-Service",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "503", description = "Employee-Service ist nicht erreichbar",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    public ResponseEntity<ProjectDto> create(
            @RequestBody @Valid CreateProjectDto dto,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        ProjectDto created = service.create(dto, jwt.getTokenValue());

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
