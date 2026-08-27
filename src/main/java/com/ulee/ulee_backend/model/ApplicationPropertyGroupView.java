package com.ulee.ulee_backend.model;

import java.util.List;

/**
 * One property's worth of applications, bundled with that property's
 * capacity and current accepted count. Built in PropertyController.manageApplications()
 * so the Applications page can show a per-property "3 / 5 spots filled" line
 * and gate the Accept / "Accept up to capacity" actions accordingly.
 */
public class ApplicationPropertyGroupView {

    private Integer propertyID;
    private String propertyName;
    private Integer capacity;
    private Long acceptedCount;
    private List<ApplicationRowView> rows;

    public Integer getPropertyID() { return propertyID; }
    public void setPropertyID(Integer propertyID) { this.propertyID = propertyID; }

    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String propertyName) { this.propertyName = propertyName; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Long getAcceptedCount() { return acceptedCount; }
    public void setAcceptedCount(Long acceptedCount) { this.acceptedCount = acceptedCount; }

    public List<ApplicationRowView> getRows() { return rows; }
    public void setRows(List<ApplicationRowView> rows) { this.rows = rows; }
}