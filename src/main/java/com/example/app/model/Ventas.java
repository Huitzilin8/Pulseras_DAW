package com.example.app.model;

import java.io.Serializable;
import org.bson.types.ObjectId;

public class Ventas implements Serializable {
    private ObjectId id;
    private ObjectId usuarioId;
    private ObjectId pulseraId;

    public Ventas() {}

    // Getters and Setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public ObjectId getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(ObjectId usuarioId) {
        this.usuarioId = usuarioId;
    }

    public ObjectId getPulseraId() {
        return this.pulseraId;
    }

    public void setPulseraId(ObjectId pulseraId) {
        this.pulseraId = pulseraId;
    }
}