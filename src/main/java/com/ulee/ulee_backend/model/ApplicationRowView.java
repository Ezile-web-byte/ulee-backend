package com.ulee.ulee_backend.model;

import java.time.LocalDateTime;
import com.ulee.ulee_backend.model.ApplicationDocument;
import java.util.List;

/**
 * Flattened, display-ready view of one Application row for the
 * manage-applications page. Assembled in the controller from
 * Application + User + Property, since Application only stores
 * raw FK ids (no JPA relationships).
 */

public class ApplicationRowView {

    private Integer applicationID;
    private Integer studentID;
    private Integer propertyID;
    private String studentName;
    private String studentInitials;
    private String studentEmail;
    private String propertyName;
    private String propertyAddress;
    private String roomType;
    private String status; // raw value from Application.status, e.g. Pending / Accepted / Rejected
    private LocalDateTime applicationDate;

    private Integer yearOfStudy;
    private String fundingStatus;
    private String messageToLandlord;
    private Boolean landlordResponded;

    private List<ApplicationDocument> documents;

    public List<ApplicationDocument> getDocuments() { return documents; }
    public void setDocuments(List<ApplicationDocument> documents) { this.documents = documents; }

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

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

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

    public String getFundingStatus() { return fundingStatus; }
    public void setFundingStatus(String fundingStatus) { this.fundingStatus = fundingStatus; }

    public String getMessageToLandlord() { return messageToLandlord; }
    public void setMessageToLandlord(String messageToLandlord) { this.messageToLandlord = messageToLandlord; }

    public Boolean getLandlordResponded() { return landlordResponded; }
    public void setLandlordResponded(Boolean landlordResponded) { this.landlordResponded = landlordResponded; }
}