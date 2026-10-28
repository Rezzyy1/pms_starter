package de.szut.pms.project;

import java.time.LocalDate;

public record ProjectDto(
    Long id,
    String name,
    Long responsibleEmployeeId,
    Long customerId,
    String customerContactName,
    String comment,
    LocalDate startDate,
    LocalDate endDate
) {

}
