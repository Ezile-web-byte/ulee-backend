package com.ulee.ulee_backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Flattened, display-ready view of one Application row for the
 * manage-applications table. Assembled in the controller from
 * Application + User + Property + Student, since Application only stores
 * raw FK ids (no JPA relationships).
 */
public class ApplicationRowView {

    private Integer applicationID;
    private Integer studentID;
    private Integer propertyID;
    private String studentName;
    private String studentInitials;
    private String propertyName;
    private String propertyAddress;
    private String roomType;
    private String status; // raw value from Application.status, e.g. PENDING / ACCEPTED / REJECTED
    private LocalDateTime applicationDate;

    // Student profile info surfaced for the landlord's review modal
    private Integer yearOfStudy;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String fundingStatus;
    private String housingPreferences;

    public Integer getApplicationID() { return applicationID; }
    public void setApplicationID(Integer applicationID) { this.applicationID = applicationID; }

    public Integer getStudentID() { return studentID; }
    public void setStudentID(Integer studentID) { this.studentID = studentID; }

    public Integer getPropertyID() { return propertyID; }
    public void setPropertyID(Integer propertyID) { this.propertyID = propertyID; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentInitials() { return studentInitials; }
    public void setStudentInitials(String studentInitials) { this.studentInitials = studentInitials; }

    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String propertyName) { this.propertyName = propertyName; }

    public String getPropertyAddress() { return propertyAddress; }
    public void setPropertyAddress(String propertyAddress) { this.propertyAddress = propertyAddress; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getApplicationDate() { return applicationDate; }
    public void setApplicationDate(LocalDateTime applicationDate) { this.applicationDate = applicationDate; }

    public Integer getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(Integer yearOfStudy) { this.yearOfStudy = yearOfStudy; }

    public BigDecimal getBudgetMin() { return budgetMin; }
    public void setBudgetMin(BigDecimal budgetMin) { this.budgetMin = budgetMin; }

    public BigDecimal getBudgetMax() { return budgetMax; }
    public void setBudgetMax(BigDecimal budgetMax) { this.budgetMax = budgetMax; }

    public String getFundingStatus() { return fundingStatus; }
    public void setFundingStatus(String fundingStatus) { this.fundingStatus = fundingStatus; }

    public String getHousingPreferences() { return housingPreferences; }
    public void setHousingPreferences(String housingPreferences) { this.housingPreferences = housingPreferences; }
}