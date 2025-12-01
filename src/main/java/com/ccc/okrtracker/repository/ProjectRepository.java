package com.ccc.okrtracker.repository;

import com.ccc.okrtracker.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // ADDED Import

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // ADDED: Method to find project by title for efficient import deduplication
    Optional<Project> findByTitle(String title);

    // We fetch all because we do in-memory filtering in frontend for tree
    // But specific queries can be added here
}