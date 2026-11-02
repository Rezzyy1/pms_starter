package de.szut.pms.project;

import java.util.List;

public record ProjectEmployeesDto(
        Long projectId,
        String projectName,
        List<ProjectEmployeeDto> employees
) {
}
