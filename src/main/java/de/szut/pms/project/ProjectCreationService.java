package de.szut.pms.project;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import de.szut.pms.employee.EmployeeClient;
import de.szut.pms.customer.CustomerClient;

@Service
public class ProjectCreationService {

    private final ProjectRepository repository;
    private final ProjectMapper mapper;
    private final EmployeeClient employeeClient;
    private final CustomerClient customerClient;

    public ProjectCreationService(
            ProjectRepository repository,
            ProjectMapper mapper,
            EmployeeClient employeeClient,
            CustomerClient customerClient) {

        this.repository = repository;
        this.mapper = mapper;
        this.employeeClient = employeeClient;
        this.customerClient = customerClient;
    }

    @Transactional 
    public ProjectDto create(CreateProjectDto dto, String token) {
        if (!employeeClient.existsById(dto.responsibleEmployeeId(), token)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Mitarbeiter mit der ID "
                        + dto.responsibleEmployeeId()
                        + " wurde nicht gefunden.");
        }

        if (!customerClient.existsById(dto.customerId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Kunde mit der ID "
                            + dto.customerId()
                            + " wurde nicht gefunden.");
        }
        
        ProjectEntity project = mapper.toEntity(dto);
        ProjectEntity saved = repository.save(project);

        return mapper.toDto(saved);
    }
}
