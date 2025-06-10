// File: src/main/java/com/example/app/controller/ChatController.java
package com.example.app.controller;

import com.example.app.dao.ChatDAO;
import com.example.app.dao.MensajeDAO;
import com.example.app.dao.SesionDAO;
import com.example.app.model.Chat;
import com.example.app.model.Mensaje;
import com.example.app.model.Sesion;
import com.example.app.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static spark.Spark.*;

public class ChatController {

    private final ChatDAO chatDao;
    private final MensajeDAO mensajeDao;
    private final SesionDAO sesionDao;
    private static final ObjectMapper jackson = new ObjectMapper();

    public ChatController(ChatDAO chatDao, MensajeDAO mensajeDao, SesionDAO sesionDao) {
        this.chatDao = chatDao;
        this.mensajeDao = mensajeDao;
        this.sesionDao = sesionDao;
    }

    public void registerRoutes() {

        // Get or create a chat session for the current browser session.
        // This is the main entry point for a user starting a chat.
        get("/api/chat/session", (req, res) -> {
            res.type("application/json");
            String sparkSessionId = req.session(true).id(); // Get or create a Spark session
            Usuario currentUser = req.session().attribute("usuario");

            Optional<Sesion> sesionOpt = sesionDao.findById(sparkSessionId);

            if (sesionOpt.isPresent()) {
                // Session exists, find its chat
                ObjectId chatId = sesionOpt.get().getChatId();
                return jackson.writeValueAsString(chatDao.findById(chatId).orElse(null));
            } else {
                // No session exists, so we create a new Chat and a new Sesion
                Chat newChat = new Chat();
                newChat.setActivo(true);
                newChat.setFechaUltimoMensaje(Instant.now());
                chatDao.create(newChat);

                Sesion newSesion = new Sesion();
                newSesion.setSessionId(sparkSessionId);
                newSesion.setChatId(newChat.getId());
                newSesion.setFechaCreacion(Instant.now());
                if (currentUser != null) {
                    newSesion.setUsuarioId(currentUser.getId());
                }
                sesionDao.create(newSesion);

                return jackson.writeValueAsString(newChat);
            }
        });

        // Get all messages for a specific chat
        get("/api/chat/:chatId/messages", (req, res) -> {
            res.type("application/json");
            // Production code needs to verify the user has access to this chat!
            ObjectId chatId = new ObjectId(req.params(":chatId"));
            List<Mensaje> messages = mensajeDao.findByChatId(chatId);
            return jackson.writeValueAsString(messages);
        });

        // Post a new message to a chat
        post("/api/chat/:chatId/messages", (req, res) -> {
            res.type("application/json");
            // Production code needs access verification here too.
            ObjectId chatId = new ObjectId(req.params(":chatId"));
            Usuario currentUser = req.session().attribute("usuario");

            Map<String, String> body = jackson.readValue(req.body(), Map.class);

            Mensaje newMessage = new Mensaje();
            newMessage.setChatId(chatId);
            newMessage.setContenido(body.get("contenido"));
            newMessage.setFecha(Instant.now());

            if (currentUser != null) {
                newMessage.setRemitenteId(currentUser.getId());
                if ("admin".equals(currentUser.getRol())) {
                    newMessage.setEsAdmin(true);
                }
            }

            mensajeDao.create(newMessage);
            chatDao.updateLastMessageDate(chatId, Instant.now()); // Assumes DAO method exists

            res.status(201);
            return jackson.writeValueAsString(newMessage);
        });

        // --- ADMIN ROUTES ---

        // Get all active chats (for admin dashboard)
        get("/api/chats/active", (req, res) -> {
            res.type("application/json");
            Usuario currentUser = req.session().attribute("usuario");
            if (currentUser == null || !"admin".equals(currentUser.getRol())) {
                halt(403, "Forbidden");
            }
            List<Chat> activeChats = chatDao.findActive(); // Assumes DAO method exists
            return jackson.writeValueAsString(activeChats);
        });
    }
}