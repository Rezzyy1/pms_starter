package de.szut.pms.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AddProjectEmployeeDto(

        @NotNull
        @Positive
        Long employeeId,

        @NotBlank
        @Size(max = 255)
        String qualification

) {
}
