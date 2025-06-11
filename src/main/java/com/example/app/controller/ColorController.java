// File: src/main/java/com/example/app/controller/ColorController.java
package com.example.app.controller;

import com.example.app.dao.ColorDAO;
import com.example.app.model.Color;
import com.example.app.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import static spark.Spark.*;

public class ColorController {

    private final ColorDAO colorDao;
    private static final ObjectMapper jackson = new ObjectMapper();

    public ColorController(ColorDAO colorDao) {
        this.colorDao = colorDao;
    }

    public void registerRoutes() {
        // GET all colors
        get("/api/admin/colors", (req, res) -> {
            res.type("application/json");
            List<Color> colors = colorDao.listAll();
            return jackson.writeValueAsString(colors);
        });

        // POST a new color (Admin)
        post("/api/admin/colors", (req, res) -> {
            res.type("application/json");
            Color newColor = jackson.readValue(req.body(), Color.class);
            colorDao.create(newColor);
            res.status(201);
            return jackson.writeValueAsString(newColor);
        });
    }
}