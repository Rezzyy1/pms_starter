package de.szut.pms.project;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Vollständige Projektdaten für PUT. Alle Felder müssen übermittelt werden.")
public record UpdateProjectDto(

        @Schema(description = "Neue Bezeichnung des Projekts; darf nicht leer sein.", example = "Kundenportal",
                minLength = 1, maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 255)
        String name,

        @Schema(description = "ID des verantwortlichen Mitarbeiters; geänderte IDs werden im Employee-Service geprüft.",
                example = "1", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Positive
        Long responsibleEmployeeId,

        @Schema(description = "ID des beauftragenden Kunden; geänderte IDs werden über die Kundenprüfung geprüft.",
                example = "2", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Positive
        Long customerId,

        @Schema(description = "Name des Ansprechpartners beim Kunden; darf nicht leer sein.", example = "Anna Müller",
                minLength = 1, maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 255)
        String customerContactName,

        @Schema(description = "Kommentar zum Projektziel. Muss vorhanden sein, darf aber eine leere Zeichenfolge sein.",
                example = "Entwicklung eines Kundenportals", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        String comment,

        @Schema(description = "Erster Projekttag im Format YYYY-MM-DD.", type = "string", format = "date",
                example = "2026-11-01", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        LocalDate startDate,

        @Schema(description = "Letzter Projekttag; darf nicht vor dem Startdatum liegen.", type = "string", format = "date",
                example = "2026-12-15", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        LocalDate endDate

) {
}
