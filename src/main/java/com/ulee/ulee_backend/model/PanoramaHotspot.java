package com.ulee.ulee_backend.model;
import jakarta.persistence.*;

@Entity
@Table(name = "panorama_hotspot")
public class PanoramaHotspot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer hotspotID;

    private Integer sourcePanoramaID;
    private Integer targetPanoramaID;
    private String label;
    private Float yaw;
    private Float pitch;

    public Integer getHotspotID() { return hotspotID; }
    public void setHotspotID(Integer hotspotID) { this.hotspotID = hotspotID; }

    public Integer getSourcePanoramaID() { return sourcePanoramaID; }
    public void setSourcePanoramaID(Integer sourcePanoramaID) { this.sourcePanoramaID = sourcePanoramaID; }

    public Integer getTargetPanoramaID() { return targetPanoramaID; }
    public void setTargetPanoramaID(Integer targetPanoramaID) { this.targetPanoramaID = targetPanoramaID; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Float getYaw() { return yaw; }
    public void setYaw(Float yaw) { this.yaw = yaw; }

    public Float getPitch() { return pitch; }
    public void setPitch(Float pitch) { this.pitch = pitch; }
}
