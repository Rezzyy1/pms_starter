package de.szut.pms.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Mitarbeiter-ID und Qualifikation für eine neue Projektzuordnung.")
public record AddProjectEmployeeDto(

        @Schema(description = "Positive ID eines vorhandenen Mitarbeiters im Employee-Service.", example = "11",
                minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Positive
        Long employeeId,

        @Schema(description = "Qualifikation des Mitarbeiters. Muss exakt mit einem hinterlegten skill übereinstimmen; "
                + "Groß- und Kleinschreibung werden berücksichtigt.", example = "Java",
                minLength = 1, maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 255)
        String qualification

) {
}
