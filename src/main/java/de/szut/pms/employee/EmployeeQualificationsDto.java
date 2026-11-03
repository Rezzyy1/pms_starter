package de.szut.pms.employee;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EmployeeQualificationsDto(List<QualificationDto> skillSet) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QualificationDto(String skill) {
    }
}
