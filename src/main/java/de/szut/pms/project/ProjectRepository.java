package de.szut.pms.project;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository
        extends JpaRepository<ProjectEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "employees")
    List<ProjectEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "employees")
    Optional<ProjectEntity> findById(Long id);

    @Query("""
            select count(project) > 0
            from ProjectEntity project
            join project.employees employee
            where employee.employeeId = :employeeId
              and project.id <> :excludedProjectId
              and project.startDate <= :endDate
              and project.endDate >= :startDate
            """)
    boolean existsOverlappingProjectForEmployee(
            @Param("employeeId") Long employeeId,
            @Param("excludedProjectId") Long excludedProjectId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            select new de.szut.pms.project.EmployeeProjectDto(
                project.id, project.name, project.startDate, project.endDate, employee.qualification)
            from ProjectEntity project
            join project.employees employee
            where employee.employeeId = :employeeId
            order by project.startDate, project.id
            """)
    List<EmployeeProjectDto> findProjectsByEmployeeId(@Param("employeeId") Long employeeId);
}
