// File: src/main/java/com/example/app/controller/ChatController.java
package com.example.app.controller;

import com.example.app.dao.ChatDAO;
import com.example.app.model.Chat;
import com.example.app.model.Usuario; // Assuming this is your User model for roles
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static spark.Spark.*;

public class ChatController {

    private final ChatDAO chatDao;
    private static final ObjectMapper jackson = new ObjectMapper();

    public ChatController(ChatDAO chatDao) {
        this.chatDao = chatDao;
        // Configure Jackson to handle dates properly
        jackson.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    public void registerRoutes() {

        // === SECURITY FILTER ===
        // This filter protects all chat-related routes.
        // It assumes that only admins should be able to view or manage chat logs directly.
        // A separate controller or WebSocket handler would manage user-facing chat interactions.
        before("/api/chats/*", (req, res) -> {
            Usuario currentUser = req.session().attribute("user");

            if (currentUser == null || !"admin".equals(currentUser.getRol())) {
                halt(403, jackson.writeValueAsString(Map.of("error", "Forbidden: Admin access required")));
            }
        });


        // === ADMIN-ONLY ROUTES ===

        // GET all active chats
        get("/api/chats/active", (req, res) -> {
            res.type("application/json");
            try {
                List<Chat> activeChats = chatDao.findActiveChats();
                return jackson.writeValueAsString(activeChats);
            } catch (Exception e) {
                res.status(500);
                return jackson.writeValueAsString(Map.of("error", "Failed to retrieve active chats: " + e.getMessage()));
            }
        });

        // GET a single chat by its MongoDB ObjectId
        get("/api/chats/:id", (req, res) -> {
            res.type("application/json");
            try {
                String id = req.params(":id");
                Optional<Chat> chatOpt = chatDao.findById(new ObjectId(id));

                if (chatOpt.isPresent()) {
                    return jackson.writeValueAsString(chatOpt.get());
                } else {
                    res.status(404); // Not Found
                    return jackson.writeValueAsString(Map.of("error", "Chat not found with that ID"));
                }
            } catch (IllegalArgumentException e) {
                res.status(400); // Bad Request
                return jackson.writeValueAsString(Map.of("error", "Invalid chat ID format"));
            }
        });

        // GET a single chat by its session ID
        get("/api/chats/session/:sessionId", (req, res) -> {
            res.type("application/json");
            try {
                String sessionId = req.params(":sessionId");
                Optional<Chat> chatOpt = chatDao.findBySessionId(sessionId);

                if (chatOpt.isPresent()) {
                    return jackson.writeValueAsString(chatOpt.get());
                } else {
                    res.status(404); // Not Found
                    return jackson.writeValueAsString(Map.of("error", "Chat not found with that session ID"));
                }
            } catch (Exception e) {
                res.status(500);
                return jackson.writeValueAsString(Map.of("error", "An error occurred: " + e.getMessage()));
            }
        });

        // POST (create) a new chat session.
        // This might be initiated by a user, so the security could be relaxed here if needed.
        post("/api/chats", (req, res) -> {
            res.type("application/json");
            try {
                Chat newChat = jackson.readValue(req.body(), Chat.class);

                if (newChat.getSessionId() == null || newChat.getSessionId().isBlank()) {
                    res.status(400); // Bad Request
                    return jackson.writeValueAsString(Map.of("error", "Field 'sessionId' is required."));
                }

                chatDao.create(newChat);
                res.status(201); // 201 Created
                return jackson.writeValueAsString(newChat);
            } catch (Exception e) {
                res.status(400); // Bad Request on malformed JSON
                return jackson.writeValueAsString(Map.of("error", "Invalid request data: " + e.getMessage()));
            }
        });

        // PUT (update) an existing chat's status
        put("/api/chats/:id", (req, res) -> {
            res.type("application/json");
            try {
                String id = req.params(":id");
                Chat updatedInfo = jackson.readValue(req.body(), Chat.class);

                // Ensure we're updating the correct chat
                updatedInfo.setId(new ObjectId(id));

                chatDao.update(updatedInfo);
                return jackson.writeValueAsString(updatedInfo);
            } catch (Exception e) {
                res.status(400);
                return jackson.writeValueAsString(Map.of("error", "Invalid update data: " + e.getMessage()));
            }
        });
    }
}
