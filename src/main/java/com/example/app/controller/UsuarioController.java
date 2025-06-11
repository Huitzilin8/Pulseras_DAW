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
        get("/api/usuario/profile/favoritos", (req, res) -> {
            res.type("application/json");

            // 1. Verificar autenticación
            Usuario currentUser = req.session().attribute("usuario");
            if (currentUser == null) {
                halt(401, jackson.writeValueAsString(Map.of("error", "Unauthorized: Please log in")));
            }

            try {
                // 2. Obtener usuario actualizado desde la base de datos
                Optional<Usuario> userOpt = usuarioDao.findById(currentUser.getId());
                if (userOpt.isEmpty()) {
                    halt(404, jackson.writeValueAsString(Map.of("error", "User not found")));
                }
                currentUser = userOpt.get();

                // --- Crucial logging here ---
                System.out.println("--- After DB Fetch ---");
                System.out.println("Retrieved User ID: " + currentUser.getId());
                System.out.println("Retrieved User Name: " + currentUser.getNombreUsuario()); // Sanity check
                System.out.println("Retrieved Favoritos IDs: " + currentUser.getFavoritosId());
                System.out.println("Is Favoritos IDs list null? " + (currentUser.getFavoritosId() == null));
                System.out.println("Is Favoritos IDs list empty? " + (currentUser.getFavoritosId() != null && currentUser.getFavoritosId().isEmpty()));
                System.out.println("----------------------");

                // 3. Obtener la lista de IDs de pulseras favoritas
                List<ObjectId> pulseraIds = currentUser.getFavoritosId();

                // 4. Si no hay favoritos, devolver lista vacía
                if (pulseraIds == null || pulseraIds.isEmpty()) {
                    return jackson.writeValueAsString(Collections.emptyList());
                }
                // --- NEW CRUCIAL LOGGING ---
                System.out.println("--- Before pulseraDao.findByIds() ---");
                System.out.println("Passing pulseraIds to DAO: " + pulseraIds);
                System.out.println("------------------------------------");

                // 5. Buscar las pulseras correspondientes
                List<Pulsera> pulseras = pulseraDao.findByIds(pulseraIds);
                System.out.println("--- After pulseraDao.findByIds() ---");
                System.out.println("Result from pulseraDao.findByIds(): " + pulseras);
                System.out.println("Number of Pulseras found by DAO: " + (pulseras != null ? pulseras.size() : "null (or no results)"));
                System.out.println("------------------------------------");
                // --- END NEW CRUCIAL LOGGING ---
                // 6. Devolver las pulseras encontradas (puede ser menos si algunas IDs son inválidas)
                return jackson.writeValueAsString(pulseras != null ? pulseras : Collections.emptyList());

            } catch (Exception e) {
                System.err.println("Error fetching favorites: " + e.getMessage());
                halt(500, jackson.writeValueAsString(Map.of("error", "Internal server error")));
                return jackson.writeValueAsString(Map.of("error", "Internal Server Error", "message", e.getMessage()));
            }
        });


        // --- BUILDS MANAGEMENT ---

        // GET the current user's custom-built bracelets
        get("/api/usuario/profile/builds", (req, res) -> {
            res.type("application/json");

            // 1. Verificar autenticación
            Usuario currentUser = req.session().attribute("usuario");
            if (currentUser == null) {
                halt(401, jackson.writeValueAsString(Map.of("error", "Unauthorized: Please log in")));
            }

            try {
                // 2. Obtener usuario actualizado desde la base de datos (crucial for up-to-date data)
                Optional<Usuario> userOpt = usuarioDao.findById(currentUser.getId());
                if (userOpt.isEmpty()) {
                    halt(404, jackson.writeValueAsString(Map.of("error", "User not found")));
                }
                currentUser = userOpt.get();

                // --- Crucial logging for current user's builds data ---
                System.out.println("--- After DB Fetch (for Builds) ---");
                System.out.println("Retrieved User ID: " + currentUser.getId());
                System.out.println("Retrieved User Name: " + currentUser.getNombreUsuario());
                System.out.println("Retrieved Builds ID: " + currentUser.getBuildsId());
                System.out.println("-----------------------------------");
                // --- END Crucial logging ---

                // 3. Check if the user has a builds ID assigned
                ObjectId buildsId = currentUser.getBuildsId();
                if (buildsId == null) {
                    // User is logged in but doesn't have a builds collection yet (e.g., new user)
                    System.out.println("[Builds API] User has no buildsId. Returning empty list.");
                    return jackson.writeValueAsString(Collections.emptyList());
                }

                // --- Crucial logging before buildsDao lookup ---
                System.out.println("--- Before buildsDao.findById() ---");
                System.out.println("Searching for Builds document with ID: " + buildsId);
                System.out.println("-----------------------------------");
                // --- END Crucial logging ---

                Optional<Builds> optBuild = buildsDao.findById(buildsId);

                // --- Crucial logging after buildsDao lookup ---
                System.out.println("--- After buildsDao.findById() ---");
                System.out.println("Builds document found? " + optBuild.isPresent());
                System.out.println("----------------------------------");
                // --- END Crucial logging ---


                if (optBuild.isPresent()) {
                    List<ObjectId> pulseraIds = optBuild.get().getPulserasIds();

                    // --- Crucial logging before pulseraDao lookup for builds ---
                    System.out.println("--- Before pulseraDao.findByIds() (for Builds) ---");
                    System.out.println("Passing Pulsera IDs to DAO: " + pulseraIds);
                    System.out.println("-------------------------------------------------");
                    // --- END Crucial logging ---

                    // If pulseraIds is null or empty, return an empty list immediately
                    if (pulseraIds == null || pulseraIds.isEmpty()) {
                        System.out.println("[Builds API] Builds document found, but pulseraIds list is empty or null. Returning empty list.");
                        return jackson.writeValueAsString(Collections.emptyList());
                    }

                    List<Pulsera> pulseras = pulseraDao.findByIds(pulseraIds);

                    // --- Crucial logging after pulseraDao lookup for builds ---
                    System.out.println("--- After pulseraDao.findByIds() (for Builds) ---");
                    System.out.println("Result from pulseraDao.findByIds(): " + pulseras);
                    System.out.println("Number of Pulseras found for builds: " + (pulseras != null ? pulseras.size() : "null (or no results)"));
                    System.out.println("-------------------------------------------------");
                    // --- END Crucial logging ---

                    // Return the found pulseras (or an empty list if null)
                    return jackson.writeValueAsString(pulseras != null ? pulseras : Collections.emptyList());
                } else {
                    // If the buildsId exists but the Builds document itself is not found
                    System.out.println("[Builds API] Builds ID exists, but document not found. Returning empty list.");
                    return jackson.writeValueAsString(Collections.emptyList());
                }
            } catch (Exception e) {
                System.err.println("Error loading user builds: " + e.getMessage());
                e.printStackTrace(); // Log the stack trace for detailed debugging

                halt(500, jackson.writeValueAsString(Map.of("error", "Internal Server Error", "message", "An unexpected error occurred while fetching your custom builds. Please try again.")));
                return null; // halt stops execution, so this return is technically unreachable but good practice for clarity.
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