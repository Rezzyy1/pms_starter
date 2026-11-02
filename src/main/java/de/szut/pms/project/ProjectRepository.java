package de.szut.pms.project;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository
        extends JpaRepository<ProjectEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "employees")
    List<ProjectEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "employees")
    Optional<ProjectEntity> findById(Long id);
}
