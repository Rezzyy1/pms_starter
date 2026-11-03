package de.szut.pms.project;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Projekt eines Mitarbeiters mit dessen Qualifikation aus der jeweiligen Zuordnung.")
public record EmployeeProjectDto(
        @Schema(description = "ID des Projekts.", example = "1", minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,
        @Schema(description = "Bezeichnung des Projekts.", example = "Kundenportal", maxLength = 255,
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "Erster Projekttag.", type = "string", format = "date", example = "2026-11-01",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate startDate,
        @Schema(description = "Letzter Projekttag.", type = "string", format = "date", example = "2026-12-15",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate endDate,
        @Schema(description = "Qualifikation des angefragten Mitarbeiters in diesem Projekt.", example = "Java",
                maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
        String qualification
) {
}
