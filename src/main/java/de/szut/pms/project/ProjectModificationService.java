package de.szut.pms.project;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import de.szut.pms.customer.CustomerClient;

@Service
public class ProjectModificationService {

    private final ProjectRepository repository;
    private final CustomerClient customerClient;

    public ProjectModificationService(
            ProjectRepository repository,
            CustomerClient customerClient) {

        this.repository = repository;
        this.customerClient = customerClient;
    }

    @Transactional
    public void update(Long id, UpdateProjectDto dto) {

        ProjectEntity project = repository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));

        if (dto.endDate().isBefore(dto.startDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Das Enddatum darf nicht vor dem Startdatum liegen."
            );
        }

        if (!project.getCustomerId().equals(dto.customerId())
                && !customerClient.existsById(dto.customerId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Kunde mit der ID "
                            + dto.customerId()
                            + " existiert nicht."
            );
        }

        project.setName(dto.name());
        project.setResponsibleEmployeeId(dto.responsibleEmployeeId());
        project.setCustomerId(dto.customerId());
        project.setCustomerContactName(dto.customerContactName());
        project.setComment(dto.comment());
        project.setStartDate(dto.startDate());
        project.setEndDate(dto.endDate());

        repository.save(project);
    }
}
