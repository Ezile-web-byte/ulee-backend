//package com.ulee.ulee_backend.model;
//
//import jakarta.persistence.*;
//import java.math.BigDecimal;
//
//@Entity
//@Table(name = "students")
//public class Student {
//
//    @Id
//    private Integer studentID; // NOT auto-generated — manually set to match the User's userID
//
//    private Integer yearOfStudy;
//    private BigDecimal budgetMin;
//    private BigDecimal budgetMax;
//
//    // Profile-level fields, set once by the student and reused across every
//    // application they submit. Shown to landlords in the Applications
//    // review modal so they don't have to ask separately.
//    private String fundingStatus;       // e.g. "NSFAS", "Bursary", "Self-Paying", "Private Funding"
//    @Column(columnDefinition = "VARCHAR(500)")
//    private String housingPreferences;  // free text — what the student is looking for in a place
//
//    public Integer getStudentID() { return studentID; }
//    public void setStudentID(Integer studentID) { this.studentID = studentID; }
//
//    public Integer getYearOfStudy() { return yearOfStudy; }
//    public void setYearOfStudy(Integer yearOfStudy) { this.yearOfStudy = yearOfStudy; }
//
//    public BigDecimal getBudgetMin() { return budgetMin; }
//    public void setBudgetMin(BigDecimal budgetMin) { this.budgetMin = budgetMin; }
//
//    public BigDecimal getBudgetMax() { return budgetMax; }
//    public void setBudgetMax(BigDecimal budgetMax) { this.budgetMax = budgetMax; }
//
//    public String getFundingStatus() { return fundingStatus; }
//    public void setFundingStatus(String fundingStatus) { this.fundingStatus = fundingStatus; }
//
//    public String getHousingPreferences() { return housingPreferences; }
//    public void setHousingPreferences(String housingPreferences) { this.housingPreferences = housingPreferences; }
//}

package com.ulee.ulee_backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "students")
public class Student {

    @Id
    private Integer studentID;

    private Integer yearOfStudy;

    public Integer getStudentID() { return studentID; }
    public void setStudentID(Integer studentID) { this.studentID = studentID; }

    public Integer getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(Integer yearOfStudy) { this.yearOfStudy = yearOfStudy; }
}