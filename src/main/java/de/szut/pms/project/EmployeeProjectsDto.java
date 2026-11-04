package de.szut.pms.project;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Einmalige Mitarbeiter-ID und Liste der Projekte dieses Mitarbeiters.")
public record EmployeeProjectsDto(
        @Schema(description = "ID des angefragten Mitarbeiters; wird einmal außerhalb der Projektliste ausgegeben.",
                example = "11", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long employeeId,
        @Schema(description = "Projekte mit der jeweiligen Qualifikation, nach Startdatum und Projekt-ID sortiert. "
                + "Ohne Zuordnungen wird [] zurückgegeben.", requiredMode = Schema.RequiredMode.REQUIRED)
        List<EmployeeProjectDto> projects
) {
}
