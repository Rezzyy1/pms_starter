package de.szut.pms.project;

import java.time.LocalDate;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Daten zum Anlegen eines Projekts. Die Projekt-ID wird automatisch vergeben.")
public record CreateProjectDto(
    @Schema(description = "Bezeichnung des Projekts; darf nicht leer sein.", example = "Kundenportal",
            minLength = 1, maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Die Projektbezeichnung ist erforderlich.")
    @Size(max = 255, message = "Die Projektbezeichnung darf höchstens 255 Zeichen lang sein.")
    String name,

    @Schema(description = "ID des verantwortlichen Mitarbeiters im Employee-Service.", example = "1",
            minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Die Mitarbeiter-ID ist erforderlich.")
    @Positive(message = "Die Mitarbeiter-ID muss positiv sein.")
    Long responsibleEmployeeId,

    @Schema(description = "ID des Kunden, der das Projekt beauftragt hat.", example = "2",
            minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Die Kunden-ID ist erforderlich.")
    @Positive(message = "Die Kunden-ID muss positiv sein.")
    Long customerId,

    @Schema(description = "Name des Ansprechpartners beim Kunden; darf nicht leer sein.", example = "Anna Müller",
            minLength = 1, maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Der Name des Ansprechpartners ist erforderlich.")
    @Size(max = 255, message = "Der Name des Ansprechpartners darf höchstens 255 Zeichen lang sein.")
    String customerContactName,

    @Schema(description = "Kommentar zum Projektziel; darf nicht leer sein.",
            example = "Entwicklung eines Kundenportals", minLength = 1,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Der Kommentar zum Projektziel ist erforderlich.")
    String comment,

    @Schema(description = "Erster Projekttag im Format YYYY-MM-DD.", type = "string", format = "date",
            example = "2026-11-01", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Das Startdatum ist erforderlich.")
    LocalDate startDate,

    @Schema(description = "Letzter Projekttag; darf nicht vor dem Startdatum liegen.", type = "string", format = "date",
            example = "2026-12-15", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Das Enddatum ist erforderlich.")
    LocalDate endDate) {

        @Schema(hidden = true)
        @AssertTrue(message = "Das Enddatum darf nicht vor dem Startdatum liegen.")
        @JsonIgnore 
        public boolean isDateRangeValid() {
            if (startDate == null || endDate == null) {
                return true;
            }

            return !endDate.isBefore(startDate);
        }
}
