package com.ulee.ulee_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "savedproperty")
public class SavedProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer savedID;

    private Integer studentID;
    private Integer propertyID;
    private Boolean savedStatus;

    public Integer getSavedID() { return savedID; }
    public void setSavedID(Integer savedID) { this.savedID = savedID; }

    public Integer getStudentID() { return studentID; }
    public void setStudentID(Integer studentID) { this.studentID = studentID; }

    public Integer getPropertyID() { return propertyID; }
    public void setPropertyID(Integer propertyID) { this.propertyID = propertyID; }

    public Boolean getSavedStatus() { return savedStatus; }
    public void setSavedStatus(Boolean savedStatus) { this.savedStatus = savedStatus; }
}