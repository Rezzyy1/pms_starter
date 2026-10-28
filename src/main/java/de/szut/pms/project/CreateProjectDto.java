package de.szut.pms.project;

import java.time.LocalDate;

public record CreateProjectDto(
    String name,
    Long responsibleEmployeeId,
    Long customerId,
    String customerContactName,
    String comment,
    LocalDate startDate,
    LocalDate endDate) {
}
