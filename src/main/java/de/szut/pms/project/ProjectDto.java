package de.szut.pms.project;

import java.time.LocalDate;
import java.util.List;

public record ProjectDto(
    Long id,
    String name,
    Long responsibleEmployeeId,
    Long customerId,
    String customerContactName,
    String comment,
    LocalDate startDate,
    LocalDate endDate,
    List<ProjectEmployeeDto> employees
) {

}
