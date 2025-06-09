package com.example.app.model;

import java.io.Serializable;
import org.bson.types.ObjectId;

public class Material implements Serializable {
    private ObjectId id;
    private String nombre;
    private String descripcion;
    private String tipo;
    private ObjectId colorId;
    private int tamanoMm;
    private int cantidadInventario;
    private String rutaImagen; // Esto puede ser ruta relativa, directa o URL

    public Material() {}

    // Getters and Setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public ObjectId getColorId() {
        return colorId;
    }

    public void setColorId(ObjectId colorId) {
        this.colorId = colorId;
    }

    public int getTamanoMm() {
        return tamanoMm;
    }

    public void setTamanoMm(int tamanoMm) {
        this.tamanoMm = tamanoMm;
    }

    public int getCantidadInventario() {
        return cantidadInventario;
    }

    public void setCantidadInventario(int cantidadInventario) {
        this.cantidadInventario = cantidadInventario;
    }

    public String getRutaImagen() {
        return rutaImagen;
    }

    public void setRutaImagen(String rutaImagen) {
        this.rutaImagen = rutaImagen;
    }
}