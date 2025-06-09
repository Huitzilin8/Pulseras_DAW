package com.example.app.controller;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.bson.types.ObjectId;
import com.example.app.dao.UsuarioDAO;
import com.example.app.model.Usuario;
import spark.Spark;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static spark.Spark.*;

public class UsuarioController {

    private final UsuarioDAO dao;

    // Custom Jackson ObjectMapper to handle ObjectId serialization/deserialization
    private static final ObjectMapper jackson = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        // Add custom serializer and deserializer for ObjectId
        module.addSerializer(ObjectId.class, new ObjectIdSerializer());
        module.addDeserializer(ObjectId.class, new ObjectIdDeserializer());
        mapper.registerModule(module);
        return mapper;
    }


    public UsuarioController(UsuarioDAO dao) {
        this.dao = dao;
    }

    public void registerRoutes() {
        // Protect admin routes by checking the user's role
        before("/api/usuario/*", (req, res) -> {
            System.out.println("[UsuarioController] Incoming request to /api/usuario/*");
            Usuario u = req.session().attribute("usuario"); // Assumes session attribute is named "usuario"
            if (u == null || !"admin".equals(u.getRol())) {
                String username = (u != null) ? u.getNombreUsuario() : "null";
                System.out.println("[UsuarioController] Unauthorized access attempt to admin route by user: " + username);
                halt(403, jackson.writeValueAsString(Map.of("error", "Forbidden")));
            } else {
                System.out.println("[UsuarioController] Admin user " + u.getNombreUsuario() + " authorized.");
            }
        });

        // Get a list of all users
        get("/api/usuarios", (req, res) -> {
            res.type("application/json");
            List<Usuario> usuarios = dao.listAll();
            System.out.println("[UsuarioController] Users fetched: " + usuarios.size());
            try {
                return jackson.writeValueAsString(usuarios);
            } catch (Exception e) {
                System.err.println("[UsuarioController] JSON serialization failed: " + e.getMessage());
                res.status(500);
                return "{\"error\":\"Failed to serialize users\"}";
            }
        });

        // Create a new user
        post("/api/usuario", (req, res) -> {
            res.type("application/json");
            System.out.println("[UsuarioController] POST request received for /api/usuario");
            try {
                Usuario u = jackson.readValue(req.body(), Usuario.class);
                dao.create(u); // DAO sets the generated ID on the object
                res.status(201);
                System.out.println("[UsuarioController] New user created: " + u.getNombreUsuario());
                return jackson.writeValueAsString(u);
            } catch (Exception e) {
                res.status(400);
                return jackson.writeValueAsString(Map.of("error", "Invalid user data", "details", e.getMessage()));
            }
        });

        // Update an existing user
        put("/api/usuario/:id", (req, res) -> {
            res.type("application/json");
            String usuarioIdStr = req.params(":id");
            System.out.println("[UsuarioController] PUT request received for /api/usuario/" + usuarioIdStr);
            try {
                ObjectId usuarioId = new ObjectId(usuarioIdStr);
                Usuario u = dao.findById(usuarioId).orElseThrow(() -> new NoSuchElementException("User not found"));

                // Read update data from body and apply to the existing user object
                Usuario updatedInfo = jackson.readValue(req.body(), Usuario.class);
                u.setNombreUsuario(updatedInfo.getNombreUsuario());
                u.setRol(updatedInfo.getRol());
                u.setCorreo(updatedInfo.getCorreo());
                // Note: Password, favorites, and builds updates would typically be handled in separate, dedicated endpoints.

                dao.update(u);
                System.out.println("[UsuarioController] User " + usuarioIdStr + " updated.");
                return jackson.writeValueAsString(u);
            } catch (IllegalArgumentException e) {
                res.status(400);
                return jackson.writeValueAsString(Map.of("error", "Invalid user ID format"));
            } catch (NoSuchElementException e) {
                res.status(404);
                return jackson.writeValueAsString(Map.of("error", e.getMessage()));
            } catch (Exception e) {
                res.status(400);
                return jackson.writeValueAsString(Map.of("error", "Invalid update data", "details", e.getMessage()));
            }
        });

        // Delete a user
        delete("/api/usuario/:id", (req, res) -> {
            String usuarioIdStr = req.params(":id");
            System.out.println("[UsuarioController] DELETE request received for /api/usuario/" + usuarioIdStr);
            try {
                ObjectId usuarioId = new ObjectId(usuarioIdStr);
                dao.delete(usuarioId);
                res.status(204);
                System.out.println("[UsuarioController] User " + usuarioIdStr + " deleted.");
                return "";
            } catch (IllegalArgumentException e) {
                res.status(400);
                res.type("application/json");
                return jackson.writeValueAsString(Map.of("error", "Invalid user ID format"));
            }
        });
    }

    // Helper classes for Jackson
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