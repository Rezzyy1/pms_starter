package de.szut.pms.project;

import java.time.LocalDate;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import com.fasterxml.jackson.annotation.JsonIgnore;

public record CreateProjectDto(
    @NotBlank(message = "Die Projektbezeichnung ist erforderlich.")
    @Size(max = 255, message = "Die Projektbezeichnung darf höchstens 255 Zeichen lang sein.")
    String name,

    @NotNull(message = "Die Mitarbeiter-ID ist erforderlich.")
    @Positive(message = "Die Mitarbeiter-ID muss positiv sein.")
    Long responsibleEmployeeId,

    @NotNull(message = "Die Kunden-ID ist erforderlich.")
    @Positive(message = "Die Kunden-ID muss positiv sein.")
    Long customerId,

    @NotBlank(message = "Der Name des Ansprechpartners ist erforderlich.")
    @Size(max = 255, message = "Der Name des Ansprechpartners darf höchstens 255 Zeichen lang sein.")
    String customerContactName,

    @NotBlank(message = "Der Kommentar zum Projektziel ist erforderlich.")
    String comment,

    @NotNull(message = "Das Startdatum ist erforderlich.")
    LocalDate startDate,

    @NotNull(message = "Das Enddatum ist erforderlich.")
    LocalDate endDate) {

        @AssertTrue(message = "Das Enddatum darf nicht vor dem Startdatum liegen.")
        @JsonIgnore 
        public boolean isDateRangeValid() {
            if (startDate == null || endDate == null) {
                return true;
            }

            return !endDate.isBefore(startDate);
        }
}
