package com.ccc.okrtracker.repository;

import com.ccc.okrtracker.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // ADDED: Method to find project by title for efficient import deduplication
    Optional<Project> findByTitle(String title);

    // Find all active projects by their IDs
    @Query("SELECT p FROM Project p WHERE p.id IN :ids AND p.isActive = true")
    List<Project> findByIdInAndIsActiveTrue(@Param("ids") Collection<Long> ids);

    // Find all active projects
    List<Project> findByIsActiveTrue();
}