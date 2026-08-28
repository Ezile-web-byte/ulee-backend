package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Integer> {

    // Oldest first, so the timeline reads top-to-bottom like the mockup
    // ("Initial report filed" → "Follow-up report" → ...).
    List<Report> findByPropertyIDOrderByReportedAtAsc(Integer propertyID);

    long countByPropertyID(Integer propertyID);

    long countByPropertyIDAndReportedAtAfter(Integer propertyID, LocalDateTime after);
}