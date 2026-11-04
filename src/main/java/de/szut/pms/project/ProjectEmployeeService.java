package de.szut.pms.project;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import de.szut.pms.employee.EmployeeClient;

@Service
public class ProjectEmployeeService {

    private final ProjectRepository repository;
    private final EmployeeClient employeeClient;

    public ProjectEmployeeService(ProjectRepository repository, EmployeeClient employeeClient) {
        this.repository = repository;
        this.employeeClient = employeeClient;
    }

    @Transactional
    public ProjectEmployeeDto addEmployee(
            Long projectId, AddProjectEmployeeDto dto, String token) {

        ProjectEntity project = repository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        boolean alreadyAssigned = project.getEmployees().stream()
                .anyMatch(employee -> employee.getEmployeeId().equals(dto.employeeId()));

        if (alreadyAssigned) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mitarbeiter mit der ID " + dto.employeeId()
                            + " ist diesem Projekt bereits zugeordnet.");
        }

        if (!employeeClient.hasQualification(dto.employeeId(), dto.qualification(), token)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mitarbeiter mit der ID " + dto.employeeId()
                            + " besitzt die Qualifikation '" + dto.qualification() + "' nicht.");
        }

        if (repository.existsOverlappingProjectForEmployee(
                dto.employeeId(), projectId, project.getStartDate(), project.getEndDate())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mitarbeiter mit der ID " + dto.employeeId()
                            + " ist im Zeitraum vom " + project.getStartDate()
                            + " bis " + project.getEndDate()
                            + " bereits einem anderen Projekt zugeordnet.");
        }

        project.addEmployee(dto.employeeId(), dto.qualification());
        repository.save(project);

        return new ProjectEmployeeDto(dto.employeeId(), dto.qualification());
    }

    @Transactional
    public ProjectEmployeeDto removeEmployee(Long projectId, Long employeeId) {
        ProjectEntity project = repository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        ProjectEmployeeEntity assignment = project.getEmployees().stream()
                .filter(employee -> employee.getEmployeeId().equals(employeeId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Mitarbeiter mit der ID " + employeeId
                                + " ist dem Projekt mit der ID " + projectId + " nicht zugeordnet."));

        ProjectEmployeeDto removed = new ProjectEmployeeDto(
                assignment.getEmployeeId(), assignment.getQualification());

        project.getEmployees().remove(assignment);
        repository.save(project);

        return removed;
    }

    @Transactional(readOnly = true)
    public EmployeeProjectsDto findProjectsByEmployeeId(Long employeeId, String token) {
        if (employeeId == null || employeeId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Die Mitarbeiter-ID muss eine positive Zahl sein.");
        }

        if (!employeeClient.existsById(employeeId, token)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Mitarbeiter mit der ID " + employeeId + " wurde nicht gefunden.");
        }

        return new EmployeeProjectsDto(employeeId, repository.findProjectsByEmployeeId(employeeId));
    }
}
