package com.ulee.ulee_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "landlords")
public class Landlord {

    @Id
    private Integer landlordID; // NOT auto-generated — manually set to match the User's userID

    private String companyName;
    private Integer propertiesCount;

    // Admin-granted trust flag — shown as the "Accredited" stat on the
    // Review Properties dashboard. Defaults to false for new landlords;
    // an admin flips this on once they've verified the landlord.
    private Boolean verified = false;

    public Integer getLandlordID() { return landlordID; }
    public void setLandlordID(Integer landlordID) { this.landlordID = landlordID; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public Integer getPropertiesCount() { return propertiesCount; }
    public void setPropertiesCount(Integer propertiesCount) { this.propertiesCount = propertiesCount; }

    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
}