package de.szut.pms.project;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
}
