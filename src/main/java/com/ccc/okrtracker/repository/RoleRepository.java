package com.ccc.okrtracker.repository;

import com.ccc.okrtracker.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    // We fetch all because we do in-memory filtering in frontend for tree
    // But specific queries can be added here
}