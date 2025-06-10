package com.example.app.model;


import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import org.bson.types.ObjectId;

public class Sesion implements Serializable {
    private String sessionId;
    private ObjectId usuarioId; // Null si es invitado, asignado al loggearse
    private Instant fechaCreacion;
    private Instant ultimaActividad;
    private ObjectId chatId;

    public Sesion() {}

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public ObjectId getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(ObjectId usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Instant getUltimaActividad() {
        return ultimaActividad;
    }

    public void setUltimaActividad(Instant ultimaActividad) {
        this.ultimaActividad = ultimaActividad;
    }

    public ObjectId getChatId() {
        return chatId;
    }

    public void setChatId(ObjectId chatId) {
        this.chatId = chatId;
    }

    public boolean esInvitado() {
        return usuarioId == null;
    }
}