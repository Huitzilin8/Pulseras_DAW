// File: src/main/java/com/example/app/controller/UsuarioController.java
package com.example.app.controller;

import com.example.app.dao.BuildsDAO;
import com.example.app.dao.FavoritosDAO;
import com.example.app.dao.PulseraDAO;
import com.example.app.dao.UsuarioDAO;
import com.example.app.model.Favoritos;
import com.example.app.model.Pulsera;
import com.example.app.model.Usuario;
import com.example.app.model.Builds;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.bson.types.ObjectId;

import at.favre.lib.crypto.bcrypt.BCrypt;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static spark.Spark.*;

public class UsuarioController {

    // --- DAO Dependencies ---
    private final UsuarioDAO usuarioDao;
    private final FavoritosDAO favoritosDao;
    private final BuildsDAO buildsDao;
    private final PulseraDAO pulseraDao;

    private static final ObjectMapper jackson = createObjectMapper();

    // --- Updated Constructor ---
    public UsuarioController(UsuarioDAO usuarioDao, FavoritosDAO favoritosDao, BuildsDAO buildsDao, PulseraDAO pulseraDao) {
        this.usuarioDao = usuarioDao;
        this.favoritosDao = favoritosDao;
        this.buildsDao = buildsDao;
        this.pulseraDao = pulseraDao;
    }

    public void registerRoutes() {

        // =================================================================
        // ===            ADMIN-ONLY ROUTES FOR USER MANAGEMENT          ===
        // =================================================================

        // Get a list of all users
        get("/api/admin/usuarios", (req, res) -> {
            res.type("application/json");
            List<Usuario> usuarios = usuarioDao.listAll();
            return jackson.writeValueAsString(usuarios);
        });

        // NUEVO ENDPOINT: Get a specific user by ID (Admin)
        get("/api/admin/usuario/:id", (req, res) -> {
            res.type("application/json");
            try {
                ObjectId userId = new ObjectId(req.params(":id"));
                Optional<Usuario> userOptional = usuarioDao.findById(userId);

                if (userOptional.isPresent()) {
                    Usuario user = userOptional.get();
                    res.status(200);
                    return jackson.writeValueAsString(user);
                } else {
                    res.status(404); // Not Found
                    return jackson.writeValueAsString(Map.of("error", "Usuario no encontrado"));
                }
            } catch (IllegalArgumentException e) {
                // Catches if the ID format is invalid (e.g., not a valid ObjectId string)
                res.status(400); // Bad Request
                return jackson.writeValueAsString(Map.of("error", "ID de usuario inválido", "details", e.getMessage()));
            } catch (Exception e) {
                System.err.println("Error getting user by ID: " + e.getMessage());
                e.printStackTrace();
                res.status(500); // Internal Server Error
                return jackson.writeValueAsString(Map.of("error", "Error interno del servidor al obtener el usuario"));
            }
        });

        // Create a new user (Admin)
        post("/api/admin/usuario", (req, res) -> {
            res.type("application/json");
            try {
                Usuario u = jackson.readValue(req.body(), Usuario.class);
                usuarioDao.create(u);
                res.status(201);
                return jackson.writeValueAsString(u);
            } catch (Exception e) {
                res.status(400);
                return jackson.writeValueAsString(Map.of("error", "Invalid user data", "details", e.getMessage()));
            }
        });

        // Update an existing user (Admin)
        put("/api/admin/usuario/:id", (req, res) -> {
            res.type("application/json");
            // ... (your existing PUT logic is great and can remain here) ...
            try {
                ObjectId usuarioId = new ObjectId(req.params(":id"));
                Usuario u = usuarioDao.findById(usuarioId).orElseThrow(() -> new NoSuchElementException("User not found"));
                Usuario updatedInfo = jackson.readValue(req.body(), Usuario.class);
                u.setNombreUsuario(updatedInfo.getNombreUsuario());
                u.setRol(updatedInfo.getRol());
                u.setCorreo(updatedInfo.getCorreo());
                usuarioDao.update(u);
                return jackson.writeValueAsString(u);
            } catch (Exception e) {
                res.status(400);
                return jackson.writeValueAsString(Map.of("error", "Invalid request data"));
            }
        });

        // Delete a user (Admin)
        delete("/api/admin/usuario/:id", (req, res) -> {
            try {
                usuarioDao.delete(new ObjectId(req.params(":id")));
                res.status(204);
                return "";
            } catch (Exception e) {
                res.status(400);
                return jackson.writeValueAsString(Map.of("error", "Invalid user ID"));
            }
        });


        // =================================================================
        // ===        LOGGED-IN USER ROUTES FOR PROFILE MANAGEMENT       ===
        // =================================================================
        // --- FAVORITES MANAGEMENT ---

        // GET the current user's favorite bracelets
        get("/api/user/usuario/profile/favoritos", (req, res) -> {
            res.type("application/json");
            Usuario currentUser = req.session().attribute("usuario");
            if (currentUser == null) {
                halt(401, jackson.writeValueAsString(Map.of("error", "Unauthorized: Please log in")));
            }

            // Find the favorites document, then find the actual bracelets
            Optional<Favoritos> favsOpt = favoritosDao.findById(currentUser.getFavoritosId());
            if (favsOpt.isPresent()) {
                List<ObjectId> pulseraIds = favsOpt.get().getPulserasIds();
                List<Pulsera> pulseras = pulseraDao.findByIds(pulseraIds); // Assumes DAO has findByIds
                return jackson.writeValueAsString(pulseras);
            }
            return jackson.writeValueAsString(Collections.emptyList());
        });

        // POST (add) a bracelet to the user's favorites
        post("/api/usuario/profile/favoritos", (req, res) -> {
            res.type("application/json");
            Usuario currentUser = req.session().attribute("usuario");
            if (currentUser == null) {
                halt(401, jackson.writeValueAsString(Map.of("error", "Unauthorized")));
            }

            Map<String, String> body = jackson.readValue(req.body(), Map.class);
            ObjectId pulseraId = new ObjectId(body.get("pulseraId"));

            // Assumes DAO handles adding the ID to the list
            favoritosDao.addPulsera(currentUser.getFavoritosId(), pulseraId);
            res.status(200);
            return jackson.writeValueAsString(Map.of("success", true, "message", "Added to favorites"));
        });

        // DELETE a bracelet from the user's favorites
        delete("/api/usuario/profile/favoritos/:pulseraId", (req, res) -> {
            Usuario currentUser = req.session().attribute("usuario");
            if (currentUser == null) {
                halt(401, jackson.writeValueAsString(Map.of("error", "Unauthorized")));
            }

            ObjectId pulseraId = new ObjectId(req.params(":pulseraId"));

            // Assumes DAO handles removing the ID from the list
            favoritosDao.removePulsera(currentUser.getFavoritosId(), pulseraId);
            res.status(204);
            return "";
        });


        // --- BUILDS MANAGEMENT ---

        // GET the current user's custom-built bracelets
        get("/api/usuario/profile/builds", (req, res) -> {
            res.type("application/json"); // Set default content type for successful responses

            Usuario currentUser = req.session().attribute("usuario");

            if (currentUser == null) {
                res.status(401); // Set the HTTP status code
                // Ensure content type is set specifically for this error response
                res.type("application/json");
                // Return the JSON string as the response body
                return jackson.writeValueAsString(Map.of("error", "Unauthorized", "message", "Porfavor inicia sesión para ver tus builds."));
            }

            try {
                // Check if the user has a builds ID assigned
                ObjectId buildsId = currentUser.getBuildsId();
                if (buildsId == null) {
                    // User is logged in but doesn't have a builds collection yet (e.g., new user)
                    // Return an empty list, as there are no builds to show.
                    return jackson.writeValueAsString(Collections.emptyList());
                }

                Optional<Builds> optBuild = buildsDao.findById(buildsId);

                if (optBuild.isPresent()) {
                    List<ObjectId> pulseraIds = optBuild.get().getPulserasIds();
                    List<Pulsera> pulseras = pulseraDao.findByIds(pulseraIds);
                    return jackson.writeValueAsString(pulseras);
                } else {
                    // If the buildsId exists but the document itself is not found (e.g., deleted manually)
                    // This is an edge case, but good to handle.
                    return jackson.writeValueAsString(Collections.emptyList());
                }
            } catch (Exception e) {
                // Catch any other unexpected exceptions (e.g., database issues, JSON serialization errors)
                System.err.println("Error loading user builds: " + e.getMessage());
                e.printStackTrace(); // Log the stack trace for debugging

                res.status(500); // Internal Server Error
                res.type("application/json"); // Ensure content type is JSON
                return jackson.writeValueAsString(Map.of("error", "Internal Server Error", "message", "An unexpected error occurred while fetching your custom builds."));
            }
        });
        // --- PROFILE UPDATE MANAGEMENT ---

        // PUT (update) the current user's username
        put("/api/usuario/profile/username", (req, res) -> {
            res.type("application/json");
            Usuario currentUser = req.session().attribute("usuario"); // Already checked by before filter
            if (currentUser == null) { // Double check for safety
                halt(401, jackson.writeValueAsString(Map.of("error", "Unauthorized")));
            }

            try {
                Map<String, String> body = jackson.readValue(req.body(), Map.class);
                String newUsername = body.get("newUsername");

                if (newUsername == null || newUsername.trim().isEmpty()) {
                    res.status(400);
                    return jackson.writeValueAsString(Map.of("error", "El nuevo nombre de usuario no puede estar vacío."));
                }

                // Check if username already exists
                if (usuarioDao.findByUsername(newUsername).isPresent() && !usuarioDao.findByUsername(newUsername).get().getId().equals(currentUser.getId())) {
                    res.status(409); // Conflict
                    return jackson.writeValueAsString(Map.of("error", "Este nombre de usuario ya está en uso."));
                }

                currentUser.setNombreUsuario(newUsername);
                usuarioDao.update(currentUser); // Assuming update method handles saving the changes
                req.session().attribute("usuario", currentUser); // Update session with new username

                res.status(200);
                return jackson.writeValueAsString(Map.of("success", true, "message", "Nombre de usuario actualizado exitosamente."));
            } catch (Exception e) {
                System.err.println("Error updating username: " + e.getMessage());
                e.printStackTrace();
                res.status(500);
                return jackson.writeValueAsString(Map.of("error", "Error interno del servidor al actualizar el nombre de usuario."));
            }
        });

        // PUT (update) the current user's password
        put("/api/usuario/profile/password", (req, res) -> {
            res.type("application/json");
            Usuario currentUser = req.session().attribute("usuario");
            if (currentUser == null) {
                halt(401, jackson.writeValueAsString(Map.of("error", "Unauthorized")));
            }

            try {
                Map<String, String> body = jackson.readValue(req.body(), Map.class);
                String currentPassword = body.get("currentPassword");
                String newPassword = body.get("newPassword");

                if (currentPassword == null || newPassword == null || currentPassword.isEmpty() || newPassword.isEmpty()) {
                    res.status(400);
                    return jackson.writeValueAsString(Map.of("error", "Todos los campos de contraseña son obligatorios."));
                }

                // Verify current password using BCrypt.verifyer()
                boolean passwordMatch = BCrypt.verifyer().verify(
                        currentPassword.toCharArray(),
                        currentUser.getHashContrasena() // Asume que getContrasenaHash() devuelve el hash en String
                ).verified;

                if (!passwordMatch) {
                    res.status(401);
                    return jackson.writeValueAsString(Map.of("error", "La contraseña actual es incorrecta."));
                }

                // Hash the new password using BCrypt.withDefaults()
                String newPasswordHash = BCrypt.withDefaults()
                        // Puedes ajustar el costo (12) si lo necesitas.
                        // LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A) es una opción si las contraseñas son muy largas.
                        // Si no necesitas una estrategia específica para contraseñas largas, `withDefaults()` es suficiente.
                        .hashToString(12, newPassword.toCharArray());

                currentUser.setHashContrasena(newPasswordHash);
                usuarioDao.update(currentUser); // Assuming update method handles saving the changes

                res.status(200);
                return jackson.writeValueAsString(Map.of("success", true, "message", "Contraseña actualizada exitosamente."));
            } catch (Exception e) {
                System.err.println("Error updating password: " + e.getMessage());
                e.printStackTrace();
                res.status(500);
                return jackson.writeValueAsString(Map.of("error", "Error interno del servidor al actualizar la contraseña."));
            }
        });
    }


    // --- Helper classes for Jackson ObjectId ---
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(ObjectId.class, new ObjectIdSerializer());
        module.addDeserializer(ObjectId.class, new ObjectIdDeserializer());
        mapper.registerModule(module);
        return mapper;
    }

    static class ObjectIdSerializer extends JsonSerializer<ObjectId> {
        @Override
        public void serialize(ObjectId value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.toHexString());
        }
    }

    static class ObjectIdDeserializer extends JsonDeserializer<ObjectId> {
        @Override
        public ObjectId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return new ObjectId(p.getText());
        }
    }
}