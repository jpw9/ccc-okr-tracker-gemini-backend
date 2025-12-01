package com.ccc.okrtracker.repository;

import com.ccc.okrtracker.entity.KeyResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeyResultRepository extends JpaRepository<KeyResult, Long> {
    // We fetch all because we do in-memory filtering in frontend for tree
    // But specific queries can be added here
}