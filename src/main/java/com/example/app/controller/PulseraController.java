// File: src/main/java/com/example/app/controller/PulseraController.java
package com.example.app.controller;

import com.example.app.dao.BuildsDAO;
import com.example.app.dao.MaterialDAO;
import com.example.app.dao.PulseraDAO;
import com.example.app.model.Material;
import com.example.app.dao.UsuarioDAO;
import com.example.app.model.Pulsera;
import com.example.app.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static spark.Spark.*;

public class PulseraController {

    private final PulseraDAO pulseraDao;
    private final UsuarioDAO usuarioDao;
    private final BuildsDAO buildsDao;
    private final MaterialDAO materialDao;
    private static final ObjectMapper jackson = new ObjectMapper();

    // A simple helper class to represent the incoming JSON for a custom design
    private static class DesignRequest {
        public List<String> materialesIds;
        public Double circunferencia;
        // Add any other custom fields you need
    }

    public PulseraController(PulseraDAO pulseraDao, UsuarioDAO usuarioDao, BuildsDAO buildsDao, MaterialDAO materialDao) {
        this.pulseraDao = pulseraDao;
        this.usuarioDao = usuarioDao;
        this.buildsDao = buildsDao;
        this.materialDao = materialDao;

    }

    public void registerRoutes() {

        // === SECURITY FILTER for Admin Routes ===
        // This protects the standard POST, PUT, and DELETE methods.
        before("/api/pulseras/:id", (req, res) -> {
            if (req.requestMethod().equals("PUT") || req.requestMethod().equals("DELETE")) {
                checkAdmin(req);
            }
        });
        before("/api/pulseras", (req, res) -> {
            if (req.requestMethod().equals("POST")) {
                checkAdmin(req);
            }
        });


        // === PUBLIC ROUTES ===

        // GET all available bracelets (with optional filtering)
        get("/api/pulseras", (req, res) -> {
            res.type("application/json");
            try {
                // Example of a simple filter: ?maxPrice=50.0
                String maxPriceParam = req.queryParams("maxPrice");
                List<Pulsera> pulseras;

                if (maxPriceParam != null) {
                    double maxPrice = Double.parseDouble(maxPriceParam);
                    //pulseras = pulseraDao.findAvailableByMaxPrice(maxPrice); // Assumes DAO method exists
                    pulseras = pulseraDao.findAvailable();
                } else {
                    pulseras = pulseraDao.findAvailable(); // Assumes DAO method exists
                }

                return jackson.writeValueAsString(pulseras);
            } catch (Exception e) {
                res.status(500);
                return jackson.writeValueAsString(Map.of("error", "Failed to retrieve bracelets"));
            }
        });

        // GET a single bracelet by ID
        get("/api/pulseras/:id", (req, res) -> {
            res.type("application/json");
            try {
                ObjectId id = new ObjectId(req.params(":id"));
                Optional<Pulsera> pulseraOpt = pulseraDao.findById(id);

                if (pulseraOpt.isPresent()) {
                    return jackson.writeValueAsString(pulseraOpt.get());
                } else {
                    res.status(404);
                    return jackson.writeValueAsString(Map.of("error", "Bracelet not found"));
                }
            } catch (IllegalArgumentException e) {
                res.status(400);
                return jackson.writeValueAsString(Map.of("error", "Invalid bracelet ID format"));
            }
        });


        // === USER-SPECIFIC ROUTE ===

        // POST a new custom design
        post("/api/pulseras/design", (req, res) -> {
            res.type("application/json");

            // 1. Authentication: Ensure user is logged in
            Usuario currentUser = req.session().attribute("user");
            if (currentUser == null) {
                halt(401, jackson.writeValueAsString(Map.of("error", "You must be logged in to design a bracelet")));
            }

            try {
                // 2. Parse Request Body
                DesignRequest design = jackson.readValue(req.body(), DesignRequest.class);
                if (design.materialesIds == null || design.materialesIds.isEmpty()) {
                    halt(400, jackson.writeValueAsString(Map.of("error", "A design must include at least one material.")));
                }

                // 3. Business Logic: Validate materials, check inventory, and calculate price
                double calculatedPrice = 0.0;
                List<Material> materialsToUpdate = new ArrayList<>();

                for (String materialIdStr : design.materialesIds) {
                    ObjectId materialId = new ObjectId(materialIdStr);
                    Optional<Material> materialOpt = materialDao.findById(materialId);

                    if (materialOpt.isEmpty()) {
                        halt(400, jackson.writeValueAsString(Map.of("error", "Invalid material ID provided: " + materialIdStr)));
                    }

                    Material material = materialOpt.get();
                    if (material.getCantidadInventario() <= 0) {
                        halt(409, jackson.writeValueAsString(Map.of("error", "Material out of stock: " + material.getNombre())));
                    }

                    // Add material price to total and prepare for inventory update
                    //calculatedPrice += material.getPrecio(); // Assuming Material has a getPrecio() method
                    materialsToUpdate.add(material);
                }

                // 4. Create and Save the new Pulsera
                Pulsera customPulsera = new Pulsera();
                customPulsera.setMaterialesIds(design.materialesIds.stream().map(ObjectId::new).toList());
                customPulsera.setCircunferencia(design.circunferencia);
                customPulsera.setPrecio(calculatedPrice);
                customPulsera.setDescripcion("Custom design by " + currentUser.getNombreUsuario());
                customPulsera.setUserBuilt(true);
                customPulsera.setDelisted(true);
                pulseraDao.create(customPulsera); // Create the bracelet first to get its ID

                // 5. Link to User's Builds
                // This assumes your Usuario has a buildsId and your BuildsDAO has a method to add a pulsera.
                buildsDao.addPulsera(currentUser.getBuildsId(), customPulsera.getId());



                // 7. Return Response
                res.status(201);
                return jackson.writeValueAsString(customPulsera);

            } catch (Exception e) {
                // This will catch parsing errors, halt() exceptions, etc.
                // The halt() method already sets the response, so this might just log.
                System.err.println("Error during custom design processing: " + e.getMessage());
                // If the error was not a halt, return a generic error
                if (!res.raw().isCommitted()) {
                    res.status(500);
                    return jackson.writeValueAsString(Map.of("error", "An internal error occurred."));
                }
                return ""; // Return empty body if halt was already called
            }
        });


        // === ADMIN-ONLY ROUTES ===

        // POST a new pre-made bracelet (Admin)
        post("/api/admin/pulseras", (req, res) -> {
            res.type("application/json");
            try {
                Pulsera pulsera = jackson.readValue(req.body(), Pulsera.class);
                pulsera.setUserBuilt(false); // This is a store-made bracelet
                pulsera.setDelisted(false);
                pulseraDao.create(pulsera);
                res.status(201);
                return jackson.writeValueAsString(pulsera);
            } catch (Exception e) {
                res.status(400);
                return jackson.writeValueAsString(Map.of("error", "Invalid bracelet data"));
            }
        });

        // PUT (update) a bracelet's info (Admin)
        put("/api/admin/pulseras/:id", (req, res) -> {
            res.type("application/json");
            try {
                String id = req.params(":id");
                Pulsera updatedInfo = jackson.readValue(req.body(), Pulsera.class);
                updatedInfo.setId(new ObjectId(id)); // Set the ID from the URL

                pulseraDao.update(updatedInfo);
                return jackson.writeValueAsString(updatedInfo);
            } catch (Exception e) {
                res.status(400);
                return jackson.writeValueAsString(Map.of("error", "Invalid update data"));
            }
        });

        // DELETE (soft delete) a bracelet (Admin)
        delete("/api/admin/pulseras/:id", (req, res) -> {
            try {
                ObjectId id = new ObjectId(req.params(":id"));
                // Instead of a hard delete, we do a soft delete
                pulseraDao.setDelisted(id, true); // Assumes this method exists in your DAO
                res.status(204);
                return "";
            } catch (Exception e) {
                res.status(400);
                return jackson.writeValueAsString(Map.of("error", "Invalid bracelet ID"));
            }
        });
    }

    // Helper method to check for admin role
    private void checkAdmin(spark.Request req) throws Exception {
        Usuario currentUser = req.session().attribute("user");
        if (currentUser == null || !"admin".equals(currentUser.getRol())) {
            halt(403, jackson.writeValueAsString(Map.of("error", "Forbidden: Admin access required")));
        }
    }
}