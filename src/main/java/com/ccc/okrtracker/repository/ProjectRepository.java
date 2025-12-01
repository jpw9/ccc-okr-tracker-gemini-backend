package com.ccc.okrtracker.repository;

import com.ccc.okrtracker.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    // We fetch all because we do in-memory filtering in frontend for tree
    // But specific queries can be added here
}