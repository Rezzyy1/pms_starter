package de.szut.pms.project;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/projects")
public class ProjectCreationController {

    private final ProjectCreationService service;

    public ProjectCreationController(ProjectCreationService service) {
        this.service = service;
    }

    @Operation(summary = "Legt ein neues Kundenprojekt an")
    @ApiResponse(responseCode = "201", description = "Projekt wurde angelegt")
    @PostMapping
    public ResponseEntity<ProjectDto> create(
            @RequestBody @Valid CreateProjectDto dto,
            @AuthenticationPrincipal Jwt jwt) {

        ProjectDto created = service.create(dto, jwt.getTokenValue());

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}