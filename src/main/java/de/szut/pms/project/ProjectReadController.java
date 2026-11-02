package de.szut.pms.project;

import java.util.List;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/projects")
public class ProjectReadController {

    private final ProjectReadService service;

    public ProjectReadController(ProjectReadService service) {
        this.service = service;
    }

    @Operation(summary = "Ruft alle Projekte ab")
    @ApiResponse(responseCode = "200", description = "Projektliste wurde abgerufen")
    @ApiResponse(responseCode = "401", description = "Authentifizierung ist erforderlich")
    @GetMapping
    public ResponseEntity<List<ProjectDto>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @Operation(summary = "Ruft ein Projekt anhand seiner ID ab")
    @ApiResponse(responseCode = "200", description = "Projekt wurde gefunden")
    @ApiResponse(responseCode = "400", description = "Ungültige Projekt-ID",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Authentifizierung ist erforderlich")
    @ApiResponse(responseCode = "404", description = "Projekt wurde nicht gefunden",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> findById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.findById(id));
    }
}
