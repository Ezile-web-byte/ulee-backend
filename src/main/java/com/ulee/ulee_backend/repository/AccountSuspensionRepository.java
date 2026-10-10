package com.ulee.ulee_backend.repository;

import com.ulee.ulee_backend.model.AccountSuspension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AccountSuspensionRepository extends JpaRepository<AccountSuspension, Integer> {

    Optional<AccountSuspension> findFirstByUserIDAndStateInOrderByCreatedAtDesc(Integer userID, Collection<String> states);

    List<AccountSuspension> findByStateIn(Collection<String> states);

    List<AccountSuspension> findByStateAndScheduledForLessThanEqual(String state, LocalDateTime time);

    List<AccountSuspension> findByUserID(Integer userID);
}