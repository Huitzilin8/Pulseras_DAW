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
import spark.utils.IOUtils;

import java.util.ArrayList;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static spark.Spark.*;

public class PulseraController {

    private static final String RELATIVE_IMG_UPLOAD_BASE_PATH = "src/main/resources/public/img";
    private final String ABSOLUTE_IMG_UPLOAD_DIR; // Esta será la ruta absoluta final
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

        // For a real implementation, you'd also pass in MaterialDAO, ColorDAO, etc.
        // public PulseraController(PulseraDAO pulseraDao, MaterialDAO materialDao) { ... }
        // Construir la ruta absoluta en el constructor
        String currentWorkingDir = System.getProperty("user.dir");
        this.ABSOLUTE_IMG_UPLOAD_DIR = Paths.get(currentWorkingDir, RELATIVE_IMG_UPLOAD_BASE_PATH).toAbsolutePath().toString();

        // Crear el directorio si no existe
        File uploadDir = new File(this.ABSOLUTE_IMG_UPLOAD_DIR);
        if (!uploadDir.exists()) {
            try {
                Files.createDirectories(uploadDir.toPath());
                System.out.println("Created image upload directory: {}"+ this.ABSOLUTE_IMG_UPLOAD_DIR);
            } catch (IOException e) {
                System.out.println("Failed to create image upload directory: {}"+ this.ABSOLUTE_IMG_UPLOAD_DIR+ e);
                // Si el directorio no se puede crear, es un error crítico.
                // Podrías lanzar una RuntimeException o manejarlo de otra forma.
                throw new RuntimeException("Failed to initialize image upload directory", e);
            }
        } else {
            System.out.println("Image upload directory already exists: {}"+ this.ABSOLUTE_IMG_UPLOAD_DIR);
        }
        System.out.println("Image upload path initialized to: {}"+ this.ABSOLUTE_IMG_UPLOAD_DIR);
    }

    public void registerRoutes() {
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
                res.status(401); // Unauthorized
                return jackson.writeValueAsString(Map.of("error", "You must be logged in to design a bracelet"));
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

        // ===========================================
        // NUEVO ENDPOINT PARA SERVIR IMÁGENES PÚBLICAS
        // ===========================================
        get("/api/public/img/:filename", (req, res) -> {
            String filename = req.params(":filename");
            File imageFile = Paths.get(ABSOLUTE_IMG_UPLOAD_DIR, filename).toFile();

            if (imageFile.exists() && imageFile.isFile()) {
                try (InputStream is = new FileInputStream(imageFile)) {
                    res.type("image/png"); // Asumiendo que todas las imágenes son PNG
                    IOUtils.copy(is, res.raw().getOutputStream());
                    return res.raw();
                } catch (IOException e) {
                    System.out.println("Error serving image " + filename + ": "+ e.getMessage());
                    res.status(500);
                    return "Internal server error: Could not serve image.";
                }
            } else {
                System.out.println("Image file not found: " + filename);
                res.status(404);
                return "Image not found.";
            }
        });

        // POST endpoint for image upload
        post("/api/admin/upload/img", (req, res) -> {
            System.out.println("Attempting to upload a picture...");
            try {
                // Especifica el directorio de subida (ABSOLUTE_IMG_UPLOAD_DIR),
                // límites de tamaño y que se almacene en disco.
                req.attribute("org.eclipse.jetty.multipartConfig",
                        new MultipartConfigElement(ABSOLUTE_IMG_UPLOAD_DIR));

                Part filePart = req.raw().getPart("image"); // "image" es el nombre del campo en el formulario multipart
                String fileName = UUID.randomUUID().toString() + ".png"; // Asegura extensión PNG y nombre único

                // Usa la ruta ABSOLUTA para crear el archivo
                File outputFilePath = Paths.get(ABSOLUTE_IMG_UPLOAD_DIR, fileName).toFile();

                try (InputStream inputStream = filePart.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {
                    IOUtils.copy(inputStream, outputStream);
                }

                res.type("application/json");
                System.out.println("Image uploaded successfully: {}"+fileName);
                return jackson.writeValueAsString(Map.of("filename", fileName));

            } catch (Exception e) {
                System.out.println("Error during image upload"+ e); // Usa logger para un mejor manejo de logs
                res.status(500);
                return jackson.writeValueAsString(Map.of("error", "Image upload failed", "details", e.getMessage()));
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
}