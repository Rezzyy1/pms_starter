package de.szut.pms.project;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Projekt-ID, Projektbezeichnung und zugeordnete Mitarbeiter mit ihren Qualifikationen.")
public record ProjectEmployeesDto(
        @Schema(description = "ID des angefragten Projekts.", example = "1", minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long projectId,
        @Schema(description = "Bezeichnung des angefragten Projekts.", example = "Kundenportal", maxLength = 255,
                requiredMode = Schema.RequiredMode.REQUIRED)
        String projectName,
        @Schema(description = "Zugeordnete Mitarbeiter; ohne Zuordnungen wird [] zurückgegeben.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<ProjectEmployeeDto> employees
) {
}
