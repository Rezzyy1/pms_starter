package de.szut.pms.project;

import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {
    public ProjectEntity toEntity(CreateProjectDto dto) {
        return new ProjectEntity(
            dto.name(),
            dto.responsibleEmployeeId(),
            dto.customerId(),
            dto.customerContactName(),
            dto.comment(),
            dto.startDate(),
            dto.endDate());
    }

    public ProjectDto toDto(ProjectEntity entity) {
        return new ProjectDto(
            entity.getId(),
            entity.getName(),
            entity.getResponsibleEmployeeId(),
            entity.getCustomerId(),
            entity.getCustomerContactName(),
            entity.getComment(),
            entity.getStartDate(),
            entity.getEndDate()
        );
    
    }
}
