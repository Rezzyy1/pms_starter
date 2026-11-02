package de.szut.pms.project;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long responsibleEmployeeId;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private String customerContactName;

    @Column(nullable = false, columnDefinition = "text")
    private String comment;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectEmployeeEntity> employees = new ArrayList<>();

    public ProjectEntity(
            String name,
            Long responsibleEmployeeId,
            Long customerId,
            String customerContactName,
            String comment,
            LocalDate startDate,
            LocalDate endDate) {

        this.name = name;
        this.responsibleEmployeeId = responsibleEmployeeId;
        this.customerId = customerId;
        this.customerContactName = customerContactName;
        this.comment = comment;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void addEmployee(Long employeeId, String qualification) {
        employees.add(new ProjectEmployeeEntity(this, employeeId, qualification));
    }
}
