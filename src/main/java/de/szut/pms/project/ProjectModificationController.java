package de.szut.pms.project;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/projects")
public class ProjectModificationController {

    private final ProjectModificationService service;

    public ProjectModificationController(
            ProjectModificationService service) {

        this.service = service;
    }

    @Operation(summary = "Ändert ein bestehendes Projekt")
    @ApiResponse(responseCode = "204", description = "Projekt wurde aktualisiert")
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable("id") Long id,
            @RequestBody @Valid UpdateProjectDto dto) {

        service.update(id, dto);

        return ResponseEntity.noContent().build();
    }
}
