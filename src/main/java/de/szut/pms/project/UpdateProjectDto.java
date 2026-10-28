package de.szut.pms.project;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateProjectDto(

        @NotBlank
        @Size(max = 255)
        String name,

        @NotNull
        @Positive
        Long responsibleEmployeeId,

        @NotNull
        @Positive
        Long customerId,

        @NotBlank
        @Size(max = 255)
        String customerContactName,

        @NotNull
        String comment,

        @NotNull
        LocalDate startDate,

        @NotNull
        LocalDate endDate

) {
}