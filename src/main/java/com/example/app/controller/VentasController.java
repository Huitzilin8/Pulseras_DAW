// File: src/main/java/com/example/app/controller/VentaController.java
package com.example.app.controller;

import com.example.app.dao.PulseraDAO;
import com.example.app.dao.VentasDAO;
import com.example.app.model.Pulsera;
import com.example.app.model.Usuario;
import com.example.app.model.Ventas;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static spark.Spark.*;

public class VentasController {

    private final VentasDAO ventaDao;
    private final PulseraDAO pulseraDao;
    private static final ObjectMapper jackson = new ObjectMapper();

    // A helper class for the checkout request body
    private static class CheckoutRequest {
        public List<String> pulseraIds;
    }

    public VentasController(VentasDAO ventaDao, PulseraDAO pulseraDao) {
        this.ventaDao = ventaDao;
        this.pulseraDao = pulseraDao;
    }

    public void registerRoutes() {
        // --- Checkout Process ---
        post("/api/user/checkout", (req, res) -> {
            res.type("application/json");
            Usuario currentUser = req.session().attribute("usuario");
            if (currentUser == null) {
                halt(401, jackson.writeValueAsString(Map.of("error", "You must be logged in to make a purchase")));
            }

            CheckoutRequest checkout = jackson.readValue(req.body(), CheckoutRequest.class);

            // Solo se crea record de venta por el momento
            for (String pulseraIdStr : checkout.pulseraIds) {
                Ventas venta = new Ventas();
                venta.setUsuarioId(currentUser.getId());
                venta.setPulseraId(new ObjectId(pulseraIdStr));
                venta.setFechaVenta(Instant.now());
                ventaDao.create(venta);
            }

            // Here you could also trigger inventory updates.

            res.status(201);
            return jackson.writeValueAsString(Map.of("success", true, "message", "Purchase completed successfully"));
        });

        // --- Order History ---
        get("/api/user/profile/orders", (req, res) -> {
            res.type("application/json");
            Usuario currentUser = req.session().attribute("usuario");
            if (currentUser == null) {
                halt(401, jackson.writeValueAsString(Map.of("error", "Unauthorized")));
            }

            List<Ventas> ventas = ventaDao.findByUsuarioId(currentUser.getId());
            List<ObjectId> pulseraIds = ventas.stream().map(Ventas::getPulseraId).toList();
            List<Pulsera> pulseras = pulseraDao.findByIds(pulseraIds);

            // We return the detailed list of bracelets, not just the sale records.
            return jackson.writeValueAsString(pulseras);
        });
    }
}