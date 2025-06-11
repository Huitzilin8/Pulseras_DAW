// src/main/java/org/example/model/Usuario.java
package com.example.app.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

import org.bson.types.ObjectId;

public class Usuario implements Serializable {
    private ObjectId id;
    private String nombreUsuario;
    private String hashContrasena;
    private String correo;
    private String rol; // "admin" o "usuario"
    private List<ObjectId> favoritosId;
    private ObjectId buildsId;
    private ObjectId chatId;
    private Instant ultimoLogin;

    public Usuario() {}

    // Getters and Setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getHashContrasena() {
        return hashContrasena;
    }

    public void setHashContrasena(String hashContrasena) {
        this.hashContrasena = hashContrasena;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public List<ObjectId> getFavoritosId() {
        return favoritosId;
    }

    public void setFavoritosId(List<ObjectId> favoritosId) {
        this.favoritosId = favoritosId;
    }

    public ObjectId getBuildsId() {
        return buildsId;
    }

    public void setBuildsId(ObjectId buildsId) {
        this.buildsId = buildsId;
    }

    public ObjectId getChatId() {return chatId;}

    public void setChatId(ObjectId chatId) {this.chatId = chatId;}

    public Instant getUltimoLogin() {return ultimoLogin;}

    public void setUltimoLogin(Instant ultimoLogin) {this.ultimoLogin = ultimoLogin;}
}