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

        // Protect admin routes
        before("/api/usuario/*", (req, res) -> {
            Usuario u = req.session().attribute("usuario");
            if (u == null || !"admin".equals(u.getRol())) {
                halt(403, jackson.writeValueAsString(Map.of("error", "Forbidden: Admin access required")));
            }
        });

        // Get a list of all users
        get("/api/usuarios", (req, res) -> {
            res.type("application/json");
            List<Usuario> usuarios = usuarioDao.listAll();
            return jackson.writeValueAsString(usuarios);
        });

        // Create a new user (Admin)
        post("/api/usuario", (req, res) -> {
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
        put("/api/usuario/:id", (req, res) -> {
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
        delete("/api/usuario/:id", (req, res) -> {
            // ... (your existing DELETE logic is great and can remain here) ...
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
        get("/api/profile/favoritos", (req, res) -> {
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
        post("/api/profile/favoritos", (req, res) -> {
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
        delete("/api/profile/favoritos/:pulseraId", (req, res) -> {
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
        get("/api/profile/builds", (req, res) -> {
            // This would follow the exact same logic as GET /api/profile/favoritos,
            // but using buildsDao and currentUser.getBuildsId()
            res.type("application/json");
            Usuario currentUser = req.session().attribute("usuario");
            if (currentUser == null) {
                halt(401, jackson.writeValueAsString(Map.of("error", "Unauthorized: Please log in")));
            }

            // Find the builds document, then find the actual bracelets
            Optional<Builds> optBuild = buildsDao.findById(currentUser.getBuildsId());
            if (optBuild.isPresent()) {
                List<ObjectId> pulseraIds = optBuild.get().getPulserasIds();
                List<Pulsera> pulseras = pulseraDao.findByIds(pulseraIds);
                return jackson.writeValueAsString(pulseras);
            }
            return jackson.writeValueAsString(Collections.emptyList());
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