package com.example.app.model;


import java.io.Serializable;
import java.util.Date;
import org.bson.types.ObjectId;

public class Chat implements Serializable {
    private ObjectId id;
    private String sessionId;
    private Boolean activo;
    private Date fechaUltimoMensaje;

    public Chat() {}

    // Getters and Setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Date getFechaUltimoMensaje() {
        return fechaUltimoMensaje;
    }

    public void setFechaUltimoMensaje(Date fechaUltimoMensaje) {
        this.fechaUltimoMensaje = fechaUltimoMensaje;
    }
}