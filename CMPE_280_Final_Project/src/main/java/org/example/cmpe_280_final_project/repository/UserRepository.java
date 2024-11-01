package org.example.cmpe_280_final_project.repository;

import org.example.cmpe_280_final_project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}

