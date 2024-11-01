package org.example.cmpe_280_final_project.repository;

import org.example.cmpe_280_final_project.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
