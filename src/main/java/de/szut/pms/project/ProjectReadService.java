package de.szut.pms.project;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectReadService {

    private final ProjectRepository repository;
    private final ProjectMapper mapper;

    public ProjectReadService(ProjectRepository repository, ProjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDto findById(Long id) {
        ProjectEntity project = repository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        return mapper.toDto(project);
    }

    @Transactional(readOnly = true)
    public ProjectEmployeesDto findEmployeesByProjectId(Long projectId) {
        ProjectDto project = findById(projectId);
        return new ProjectEmployeesDto(project.id(), project.name(), project.employees());
    }
}
