package com.example.app.model;

import java.io.Serializable;
import java.util.List;
import org.bson.types.ObjectId;

public class Builds implements Serializable {
    private ObjectId id;
    private List<ObjectId> pulserasIds;

    public Builds() {}

    // Getters and Setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public List<ObjectId> getPulserasIds() {
        return pulserasIds;
    }

    public void setPulserasIds(List<ObjectId> pulserasIds) {
        this.pulserasIds = pulserasIds;
    }
}
