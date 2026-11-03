package de.szut.pms.project;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
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
public class ProjectModificationController {

    private final ProjectModificationService service;

    public ProjectModificationController(
            ProjectModificationService service) {

        this.service = service;
    }

    @Operation(summary = "Ändert ein bestehendes Projekt",
            description = "Erwartet sämtliche Projektdaten als JSON. Geänderte Mitarbeiter- und "
                    + "Kunden-IDs werden geprüft. Bei geändertem Start- oder Enddatum wird die Verfügbarkeit der "
                    + "zugeordneten Mitarbeiter im neuen Zeitraum geprüft.")
    @ApiResponse(responseCode = "204", description = "Projekt wurde aktualisiert; Antwort ohne Inhalt",
            content = @Content)
    @ApiResponse(responseCode = "400",
            description = "Ungültige Projekt-ID oder Eingaben, ungültiger Zeitraum oder unbekannter Mitarbeiter/Kunde",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Ein gültiger Bearer-Token ist erforderlich",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Projekt wurde nicht gefunden",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409",
            description = "Ein zugeordneter Mitarbeiter ist im neuen Zeitraum einem anderen Projekt zugeordnet",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "502", description = "Fehlerhafte Antwort des Employee-Service",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "503", description = "Employee-Service ist nicht erreichbar",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @Parameter(description = "ID des zu ändernden Projekts", example = "1") @PathVariable("id") Long id,
            @RequestBody @Valid UpdateProjectDto dto,
            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt) {

        service.update(
                id,
                dto,
                jwt.getTokenValue()
        );

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Löscht ein bestehendes Projekt",
            description = "Löscht das Projekt einschließlich seiner Mitarbeiterzuordnungen.")
    @ApiResponse(responseCode = "204", description = "Projekt wurde gelöscht; Antwort ohne Inhalt",
            content = @Content)
    @ApiResponse(responseCode = "400", description = "Ungültige Projekt-ID",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Ein gültiger Bearer-Token ist erforderlich",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Projekt wurde nicht gefunden",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(
            @Parameter(description = "ID des zu löschenden Projekts", example = "1") @PathVariable("id") Long id) {

        service.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
