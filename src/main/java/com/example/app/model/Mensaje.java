package com.example.app.model;

import org.bson.types.ObjectId;
import java.io.Serializable;
import java.time.Instant;
import java.util.Date;

public class Mensaje implements Serializable {
    private ObjectId id;
    private String contenido;
    private Instant fecha;
    private ObjectId chatId;
    private ObjectId remitenteId; // Null si es invitado, userId si es usuario loggeado
    private boolean esAdmin;      // True si lo envía un admin

    public Mensaje() {}

    // Getters and Setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public Instant getFecha() {
        return fecha;
    }

    public void setFecha(Instant fecha) {
        this.fecha = fecha;
    }

    public ObjectId getChatId() {
        return chatId;
    }

    public void setChatId(ObjectId chatId) {
        this.chatId = chatId;
    }

    public ObjectId getRemitenteId() {
        return remitenteId;
    }

    public void setRemitenteId(ObjectId remitenteId) {
        this.remitenteId = remitenteId;
    }

    public boolean isEsAdmin() {
        return esAdmin;
    }

    public void setEsAdmin(boolean esAdmin) {
        this.esAdmin = esAdmin;
    }
}
