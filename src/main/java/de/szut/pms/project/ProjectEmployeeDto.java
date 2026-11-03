package de.szut.pms.project;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mitarbeiter und seine Qualifikation in einem Projekt. "
        + "Wird auch als Antwort auf das Hinzufügen und Entfernen einer Zuordnung verwendet.")
public record ProjectEmployeeDto(
        @Schema(description = "ID des Mitarbeiters.", example = "11", minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long employeeId,
        @Schema(description = "Qualifikation, mit der der Mitarbeiter in diesem Projekt eingesetzt ist.",
                example = "Java", maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
        String qualification
) {
}
