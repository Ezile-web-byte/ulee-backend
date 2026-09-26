package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.AdminActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdminActivityLogRepository extends JpaRepository<AdminActivityLog, Integer> {

    /** Newest 5 logged admin actions, for the Dashboard's Recent Activity feed. */
    List<AdminActivityLog> findTop5ByOrderByTimestampDesc();

    /** Every logged admin action, newest first, for the full Activity Log page ("View all"). */
    List<AdminActivityLog> findAllByOrderByTimestampDesc();
}