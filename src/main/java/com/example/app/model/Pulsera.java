// File: src/main/java/model/Pulsera.java
package com.example.app.model;

import java.io.Serializable;
import java.util.List;
import org.bson.types.ObjectId;

public class Pulsera implements Serializable {
    private ObjectId id;
    private String nombre;
    private String descripcion;
    private Double circunferencia;
    private Double precio;
    private List<ObjectId> materialesIds;
    private List<ObjectId> coloresIds;
    private Boolean delisted;
    private Boolean userBuilt; // Falso == creado por admin
    private String imgURL;

    public Pulsera() {}

    // Getters and Setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getNombre() {return nombre;}

    public void setNombre(String nombre) {this.nombre = nombre;}

    public String getDescripcion() {
        return this.descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Double getCircunferencia() {
        return circunferencia;
    }

    public void setCircunferencia(Double circunferencia) {
        this.circunferencia = circunferencia;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }

    public List<ObjectId> getMaterialesIds() {
        return materialesIds;
    }

    public void setMaterialesIds(List<ObjectId> materialesIds) {
        this.materialesIds = materialesIds;
    }

    public List<ObjectId> getColoresIds() {
        return coloresIds;
    }

    public void setColoresIds(List<ObjectId> coloresIds) {
        this.coloresIds = coloresIds;
    }

    public Boolean getDelisted() {
        return delisted;
    }

    public void setDelisted(Boolean delisted) {
        this.delisted = delisted;
    }

    public Boolean getUserBuilt() {
        return userBuilt;
    }

    public void setUserBuilt(Boolean userBuilt) {
        this.userBuilt = userBuilt;
    }

    public String getImgURL() {return imgURL;}

    public void setImgURL(String imgURL) {this.imgURL = imgURL;}
}