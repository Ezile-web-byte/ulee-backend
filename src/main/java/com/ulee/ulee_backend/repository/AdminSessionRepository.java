package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.AdminSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminSessionRepository extends JpaRepository<AdminSession, Integer> {

    Optional<AdminSession> findBySessionId(String sessionId);

    List<AdminSession> findByUserIDAndActiveTrueOrderByLastActiveAtDesc(Integer userID);
}