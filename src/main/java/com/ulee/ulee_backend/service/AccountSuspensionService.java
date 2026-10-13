package com.ulee.ulee_backend.service;

import com.ulee.ulee_backend.model.AccountSuspension;
import com.ulee.ulee_backend.model.AdminActivityLog;
import com.ulee.ulee_backend.model.Notification;
import com.ulee.ulee_backend.model.Property;
import com.ulee.ulee_backend.model.User;
import com.ulee.ulee_backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * All deactivation rules live here so the admin controller and the
 * scheduled job behave identically:
 *
 *  - Deactivate  -> scheduleDeactivation(): landlord is told, account switches off after 10 minutes.
 *  - Cancel      -> cancelPending(): admin changes their mind inside the 10 minutes.
 *  - Warning cap -> applyNow(id, WARNING_LIMIT): used when a landlord passes MAX_WARNINGS.
 *  - Reactivate  -> reactivate(): turns the account back on and restores ONLY the
 *                   properties this deactivation hid.
 */
@Service
public class AccountSuspensionService {

    private static final Logger log = LoggerFactory.getLogger(AccountSuspensionService.class);

    public static final int MAX_WARNINGS = 5;            // more than this -> automatic suspension
    public static final int DEACTIVATION_DELAY_MINUTES = 10;

    public static final String PENDING = "PENDING";
    public static final String ACTIVE = "ACTIVE";
    public static final String ENDED = "ENDED";
    public static final String CANCELLED = "CANCELLED";

    public static final String REASON_ADMIN = "ADMIN";
    public static final String REASON_WARNING_LIMIT = "WARNING_LIMIT";

    @Autowired private AccountSuspensionRepository repo;
    @Autowired private UserRepository userRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private LandlordRepository landlordRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private AdminActivityLogRepository adminActivityLogRepository;

    // ------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public Optional<AccountSuspension> findOpen(Integer userID) {
        return repo.findFirstByUserIDAndStateInOrderByCreatedAtDesc(userID, List.of(PENDING, ACTIVE));
    }

    /** userID -> its PENDING/ACTIVE suspension, for the Manage Users page. */
    @Transactional(readOnly = true)
    public Map<Integer, AccountSuspension> openByUser() {
        Map<Integer, AccountSuspension> map = new HashMap<>();
        for (AccountSuspension s : repo.findByStateIn(List.of(PENDING, ACTIVE))) {
            map.put(s.getUserID(), s);
        }
        return map;
    }

    // ------------------------------------------------------------ actions

    /** Starts the 10-minute countdown. Returns the existing row if one is already open. */
    @Transactional
    public AccountSuspension scheduleDeactivation(Integer userID) {
        Optional<AccountSuspension> open = findOpen(userID);
        if (open.isPresent()) return open.get();

        LocalDateTime now = LocalDateTime.now();
        AccountSuspension s = new AccountSuspension();
        s.setUserID(userID);
        s.setState(PENDING);
        s.setReason(REASON_ADMIN);
        s.setCreatedAt(now);
        s.setScheduledFor(now.plusMinutes(DEACTIVATION_DELAY_MINUTES));
        repo.save(s);

        notifyUser(userID, "Account Will Be Deactivated",
                "Your ULEE account will be deactivated in " + DEACTIVATION_DELAY_MINUTES
                        + " minutes by an administrator. Please save any work. While deactivated, your properties "
                        + "are hidden from students and you cannot add new listings. "
                        + "If you believe this is a mistake, please contact support.");
        return s;
    }

    /** Admin stops a deactivation that has not happened yet. */
    @Transactional
    public boolean cancelPending(Integer userID) {
        Optional<AccountSuspension> open = findOpen(userID);
        if (open.isEmpty() || !PENDING.equals(open.get().getState())) return false;

        AccountSuspension s = open.get();
        s.setState(CANCELLED);
        s.setEndedAt(LocalDateTime.now());
        repo.save(s);

        notifyUser(userID, "Deactivation Cancelled",
                "The planned deactivation of your ULEE account has been cancelled. Your account stays active.");
        return true;
    }

    /**
     * Deactivates right now: blocks login (isActive=false) and hides the user's
     * properties. Used by the scheduled job and by the automatic warning limit.
     */
    @Transactional
    public void applyNow(Integer userID, String reason) {
        Optional<User> userOpt = userRepository.findById(userID);
        if (userOpt.isEmpty()) return;

        AccountSuspension s = findOpen(userID).orElseGet(AccountSuspension::new);
        if (ACTIVE.equals(s.getState())) return; // already deactivated

        LocalDateTime now = LocalDateTime.now();
        if (s.getUserID() == null) {
            s.setUserID(userID);
            s.setCreatedAt(now);
        }

        User user = userOpt.get();
        user.setIsActive(false);
        userRepository.save(user);

        // Hide only properties that are visible right now, and remember which ones.
        List<Integer> hidden = new ArrayList<>();
        if (landlordRepository.existsById(userID)) {
            List<Property> owned = ownedProperties(userID);
            for (Property p : owned) {
                if (Boolean.TRUE.equals(p.getIsAvailable())) {
                    hidden.add(p.getPropertyID());
                    p.setIsAvailable(false);
                }
            }
            propertyRepository.saveAll(owned);
        }

        s.setState(ACTIVE);
        s.setReason(reason);
        s.setAppliedAt(now);
        s.setHiddenPropertyIds(hidden.stream().map(String::valueOf).collect(Collectors.joining(",")));
        repo.save(s);

        if (REASON_WARNING_LIMIT.equals(reason)) {
            notifyUser(userID, "Account Suspended",
                    "Your ULEE account has been suspended because it went over the limit of "
                            + MAX_WARNINGS + " warnings. Your properties are hidden from students. "
                            + "An administrator will review your account.");
            logSystem("Auto-suspended", "Suspended " + displayName(user)
                    + " automatically (more than " + MAX_WARNINGS + " warnings)");
        } else {
            notifyUser(userID, "Account Deactivated",
                    "Your ULEE account has been deactivated by an administrator. Your properties are hidden "
                            + "from students and you cannot add new listings. "
                            + "If you believe this was done in error, please contact support.");
            logSystem("Deactivated", "Deactivated " + displayName(user) + " after the notice period");
        }
    }

    /**
     * Turns the account back on. Restores only the properties this deactivation hid.
     * Accounts deactivated BEFORE this feature existed have no record of what was hidden,
     * so for those every property is restored (the old behaviour).
     */
    @Transactional
    public void reactivate(Integer userID) {
        Optional<AccountSuspension> open = findOpen(userID);
        if (open.isPresent() && PENDING.equals(open.get().getState())) {
            cancelPending(userID);
            return;
        }

        Optional<User> userOpt = userRepository.findById(userID);
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();
        user.setIsActive(true);
        userRepository.save(user);

        if (landlordRepository.existsById(userID)) {
            List<Property> owned = ownedProperties(userID);
            Set<Integer> toRestore = open.map(s -> parseIds(s.getHiddenPropertyIds())).orElse(null);
            for (Property p : owned) {
                if (toRestore == null || toRestore.contains(p.getPropertyID())) {
                    p.setIsAvailable(true);
                }
            }
            propertyRepository.saveAll(owned);
        }

        open.ifPresent(s -> {
            s.setState(ENDED);
            s.setEndedAt(LocalDateTime.now());
            repo.save(s);
        });

        notifyUser(userID, "Account Reactivated",
                "Your ULEE account has been reactivated. You can log in again and your properties are visible to students.");
    }

    /** Removes this user's suspension history. Call right before deleting the user. */
    @Transactional
    public void purge(Integer userID) {
        repo.deleteAll(repo.findByUserID(userID));
    }

    // ------------------------------------------------------------ scheduled job

    /** Every 30 seconds: finish any deactivation whose 10 minutes are up. Survives server restarts (state is in the DB). */
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void processDue() {
        List<AccountSuspension> due = repo.findByStateAndScheduledForLessThanEqual(PENDING, LocalDateTime.now());
        for (AccountSuspension s : due) {
            try {
                applyNow(s.getUserID(), s.getReason() != null ? s.getReason() : REASON_ADMIN);
            } catch (Exception e) {
                log.error("Could not deactivate user {}: {}", s.getUserID(), e.getMessage(), e);
            }
        }
    }

    // ------------------------------------------------------------ helpers

    private List<Property> ownedProperties(Integer landlordID) {
        return propertyRepository.findAll().stream()
                .filter(p -> landlordID.equals(p.getLandlordID()))
                .collect(Collectors.toList());
    }

    private Set<Integer> parseIds(String csv) {
        Set<Integer> ids = new HashSet<>();
        if (csv == null || csv.isBlank()) return ids;
        for (String part : csv.split(",")) {
            try { ids.add(Integer.parseInt(part.trim())); } catch (NumberFormatException ignored) { }
        }
        return ids;
    }

    private void notifyUser(Integer userID, String title, String message) {
        Notification n = new Notification();
        if (landlordRepository.existsById(userID)) {
            n.setLandlordID(userID);
        } else if (studentRepository.existsById(userID)) {
            n.setStudentID(userID);
        }
        n.setTitle(title);
        n.setMessage(message);
        n.setCreatedAt(LocalDateTime.now());
        n.setIsRead(false);
        notificationRepository.save(n);
    }

    private void logSystem(String action, String message) {
        AdminActivityLog entry = new AdminActivityLog();
        entry.setActorName("System");
        entry.setAction(action);
        entry.setMessage(message);
        entry.setTimestamp(LocalDateTime.now());
        adminActivityLogRepository.save(entry);
    }

    private String displayName(User user) {
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? "user #" + user.getUserID() : full;
    }
}