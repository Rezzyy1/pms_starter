package de.szut.pms.project;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
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
}
