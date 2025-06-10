// File: src/main/java/com/example/app/controller/ChatController.java
package com.example.app.controller;

import com.example.app.dao.ChatDAO;
import com.example.app.dao.MensajeDAO;
import com.example.app.dao.UsuarioDAO;
import com.example.app.dao.UsuarioDAO;
import com.example.app.model.Chat;
import com.example.app.model.Mensaje;
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
    private final UsuarioDAO usuarioDao;
    private static final ObjectMapper jackson = new ObjectMapper();

    public ChatController(ChatDAO chatDao, MensajeDAO mensajeDao, UsuarioDAO usuarioDao) {
        this.chatDao = chatDao;
        this.mensajeDao = mensajeDao;
        this.usuarioDao = usuarioDao;
    }

    public void registerRoutes() {

        // GET or Create a chat session. This is the new entry point.
        get("/api/chat/session", (req, res) -> {
            res.type("application/json");
            Usuario currentUser = req.session().attribute("usuario");

            // --- Case 1: User is LOGGED IN ---
            if (currentUser != null) {
                ObjectId chatId = currentUser.getChatId();

                if (chatId != null) {
                    // User has an existing chat, return it
                    return jackson.writeValueAsString(chatDao.findById(chatId).orElse(null));
                } else {
                    // First chat for this user. Create it and link it to them.
                    Chat newChat = new Chat();
                    newChat.setActivo(true);
                    chatDao.create(newChat);

                    currentUser.setChatId(newChat.getId());
                    usuarioDao.update(currentUser); // Persist the new chatId on the user

                    return jackson.writeValueAsString(newChat);
                }
            }
            // --- Case 2: User is a GUEST ---
            else {
                ObjectId guestChatId = req.session().attribute("guest_chat_id");

                if (guestChatId != null) {
                    // Guest has a chat for this browser session, return it
                    return jackson.writeValueAsString(chatDao.findById(guestChatId).orElse(null));
                } else {
                    // First chat for this guest. Create one and save its ID to the browser session.
                    Chat newChat = new Chat();
                    newChat.setActivo(true);
                    chatDao.create(newChat);
                    req.session().attribute("guest_chat_id", newChat.getId());
                    return jackson.writeValueAsString(newChat);
                }
            }
        });

        // Get all messages for a specific chat
        get("/api/chat/:chatId/messages", (req, res) -> {
            res.type("application/json");
            // Authorization is needed here to ensure the user can access this chat
            verifyChatAccess(req);

            ObjectId chatId = new ObjectId(req.params(":chatId"));
            List<Mensaje> messages = mensajeDao.findByChatId(chatId);
            return jackson.writeValueAsString(messages);
        });

        // Post a new message to a chat
        post("/api/chat/:chatId/messages", (req, res) -> {
            res.type("application/json");
            verifyChatAccess(req); // Verify access before allowing a post

            ObjectId chatId = new ObjectId(req.params(":chatId"));
            Usuario currentUser = req.session().attribute("usuario");
            Map<String, String> body = jackson.readValue(req.body(), Map.class);

            Mensaje newMessage = new Mensaje();
            newMessage.setChatId(chatId);
            newMessage.setContenido(body.get("contenido"));
            newMessage.setFecha(Instant.now());

            if (currentUser != null) {
                newMessage.setRemitenteId(currentUser.getId());
                newMessage.setEsAdmin("admin".equals(currentUser.getRol()));
            }

            mensajeDao.create(newMessage);
            chatDao.updateLastMessageDate(chatId, Instant.now());

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
    private void verifyChatAccess(spark.Request req) {
        ObjectId requestedChatId = new ObjectId(req.params(":chatId"));
        Usuario currentUser = req.session().attribute("usuario");

        boolean hasAccess = false;
        if (currentUser != null) {
            // User is logged in. Check if they are an admin or if the chat ID matches their own.
            if ("admin".equals(currentUser.getRol()) || requestedChatId.equals(currentUser.getChatId())) {
                hasAccess = true;
            }
        } else {
            // User is a guest. Check if the chat ID matches the one in their browser session.
            ObjectId guestChatId = req.session().attribute("guest_chat_id");
            if (requestedChatId.equals(guestChatId)) {
                hasAccess = true;
            }
        }

        if (!hasAccess) {
            halt(403, "Forbidden: You do not have access to this chat.");
        }
    }
}