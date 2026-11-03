package de.szut.pms.project;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Vollständige Projektdaten einschließlich der zugeordneten Mitarbeiter.")
public record ProjectDto(
    @Schema(description = "Eindeutige, automatisch vergebene Projekt-ID.", example = "1", minimum = "1",
            accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.REQUIRED)
    Long id,
    @Schema(description = "Bezeichnung des Projekts.", example = "Kundenportal", maxLength = 255,
            requiredMode = Schema.RequiredMode.REQUIRED)
    String name,
    @Schema(description = "ID des für das Projekt verantwortlichen Mitarbeiters.", example = "1", minimum = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    Long responsibleEmployeeId,
    @Schema(description = "ID des beauftragenden Kunden.", example = "2", minimum = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    Long customerId,
    @Schema(description = "Name des Ansprechpartners beim Kunden.", example = "Anna Müller", maxLength = 255,
            requiredMode = Schema.RequiredMode.REQUIRED)
    String customerContactName,
    @Schema(description = "Kommentar zum Projektziel.", example = "Entwicklung eines Kundenportals",
            requiredMode = Schema.RequiredMode.REQUIRED)
    String comment,
    @Schema(description = "Erster Projekttag.", type = "string", format = "date", example = "2026-11-01",
            requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDate startDate,
    @Schema(description = "Letzter Projekttag.", type = "string", format = "date", example = "2026-12-15",
            requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDate endDate,
    @Schema(description = "Mitarbeiter mit ihrer jeweiligen Qualifikation im Projekt; ohne Zuordnungen eine leere Liste.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    List<ProjectEmployeeDto> employees
) {

}
