// File: src/main/java/com/example/app/controller/MaterialController.java
package com.example.app.controller;

import com.example.app.model.Material;      // Your Material model
import com.example.app.model.Usuario;       // Your Usuario model for checking roles
import com.example.app.dao.MaterialDAO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static spark.Spark.*;

public class MaterialController {

    private final MaterialDAO materialDao;
    private static final ObjectMapper jackson = new ObjectMapper();

    public MaterialController(MaterialDAO materialDao) {
        this.materialDao = materialDao;
    }

    public void registerRoutes() {
        // === PUBLIC ROUTES ===

        // GET all materials
        get("/api/public/materials", (req, res) -> {
            res.type("application/json");
            try {
                List<Material> materials = materialDao.listAll();
                return jackson.writeValueAsString(materials);
            } catch (Exception e) {
                res.status(500);
                return jackson.writeValueAsString(Map.of("error", "Failed to retrieve materials"));
            }
        });

        // GET a single material by its ID
        get("/api/public/materials/:id", (req, res) -> {
            res.type("application/json");
            try {
                String id = req.params(":id");
                Optional<Material> materialOpt = materialDao.findById(new ObjectId(id));

                if (materialOpt.isPresent()) {
                    return jackson.writeValueAsString(materialOpt.get());
                } else {
                    res.status(404); // Not Found
                    return jackson.writeValueAsString(Map.of("error", "Material not found"));
                }
            } catch (IllegalArgumentException e) {
                res.status(400); // Bad Request
                return jackson.writeValueAsString(Map.of("error", "Invalid material ID format"));
            }
        });


        // === ADMIN-ONLY ROUTES ===

        // POST (create) a new material
        post("/api/admin/materials", (req, res) -> {
            res.type("application/json");
            try {
                Material newMaterial = jackson.readValue(req.body(), Material.class);

                // --- Simple Validation Example ---
                if (newMaterial.getNombre() == null || newMaterial.getNombre().trim().isEmpty()) {
                    res.status(400); // Bad Request
                    return jackson.writeValueAsString(Map.of("error", "Material name ('nombre') is required"));
                }

                materialDao.create(newMaterial);
                res.status(201); // 201 Created
                return jackson.writeValueAsString(newMaterial);
            } catch (Exception e) {
                res.status(400); // Bad Request on malformed JSON
                return jackson.writeValueAsString(Map.of("error", "Invalid request data"));
            }
        });

        // PUT (update) an existing material
        put("/api/admin/materials/:id", (req, res) -> {
            res.type("application/json");
            try {
                String id = req.params(":id");
                Material updatedInfo = jackson.readValue(req.body(), Material.class);
                updatedInfo.setId(new ObjectId(id)); // Set the ID from the URL

                materialDao.update(updatedInfo);
                return jackson.writeValueAsString(updatedInfo);
            } catch (Exception e) {
                res.status(400);
                return jackson.writeValueAsString(Map.of("error", "Invalid update data"));
            }
        });

        // DELETE a material by ID
        delete("/api/admin/materials/:id", (req, res) -> {
            try {
                String id = req.params(":id");
                materialDao.delete(new ObjectId(id));
                res.status(204); // 204 No Content (standard for successful delete)
                return ""; // Return an empty body
            } catch (Exception e) {
                res.status(400);
                return jackson.writeValueAsString(Map.of("error", "Invalid material ID"));
            }
        });
    }
}
