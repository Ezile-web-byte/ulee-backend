package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Integer> {
}