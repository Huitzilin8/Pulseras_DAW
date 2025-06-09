package com.example.app.controller;

import com.example.app.dao.UserDAO;
import com.example.app.model.User;
import static com.example.app.constants.ColorCodes.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import static spark.Spark.*;
import java.util.List;
import java.util.Map;

public class UserController {
    private final UserDAO dao;
    private static final ObjectMapper jackson = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public UserController(UserDAO dao) { this.dao = dao; }

    public void registerRoutes() {
        // protect admin routes
        before("/api/user/*", (req, res) -> {
            System.out.println(INFO + "[UserController] " + NEUTRAL + "Incoming request to /api/user/*" + RESET);
            User u = req.session().attribute("user");
            if (u == null || !"admin".equals(u.getRole())) {
                System.out.println(INFO + "[UserController] " + ERROR + "Unauthorized access attempt to admin route by user: " +
                        (u != null ? VARIABLE + u.getUsername() : ERROR + "null") + ":" + (u != null ? u.getRole() : "") + RESET);
                try {
                    halt(403, jackson.writeValueAsString(Map.of("error", "Forbidden")));
                } catch (Exception e) {
                    halt(500);
                }
            } else {
                System.out.println(INFO + "[UserController] " + SUCCESS + "Admin user " + VARIABLE + u.getUsername() + SUCCESS + " authorized." + RESET);
            }
        });

        get("/api/users", (req, res) -> {
            res.type("application/json");
            List<User> users = dao.listAll();
            System.out.println(INFO + "[UserController] " + SUCCESS + "Users fetched: " + users.size() + RESET);

            try {
                return jackson.writeValueAsString(users);
            } catch (Exception e) {
                System.err.println(INFO + "[UserController] " + ERROR + "JSON serialization failed: " + e.getMessage() + RESET);
                res.status(500);
                return "{\"error\":\"Failed to serialize users\"}";
            }
        });

        post("/api/user", (req, res) -> {
            System.out.println(INFO + "[UserController] " + NEUTRAL + "POST request received for /api/user" + RESET);
            try {
                User u = jackson.readValue(req.body(), User.class);
                dao.create(u);
                res.status(201);
                System.out.println(INFO + "[UserController] " + SUCCESS + "New user created: " + VARIABLE + u.getUsername() + RESET);
                return jackson.writeValueAsString(u);
            } catch (Exception e) {
                res.status(400);
                return jackson.writeValueAsString(Map.of("error", "Invalid user data"));
            }
        });

        put("/api/user/:id", (req, res) -> {
            String userId = req.params(":id");
            System.out.println(INFO + "[UserController] " + NEUTRAL + "PUT request received for /api/user/" + VARIABLE + userId + RESET);
            try {
                User u = dao.findById(userId).orElseThrow();
                User upd = jackson.readValue(req.body(), User.class);
                u.setUsername(upd.getUsername());
                u.setRole(upd.getRole());
                dao.update(u);
                System.out.println(INFO + "[UserController] " + SUCCESS + "User " + VARIABLE + userId + SUCCESS + " updated." + RESET);
                return jackson.writeValueAsString(u);
            } catch (Exception e) {
                res.status(404);
                System.out.println(INFO + "[UserController] " + ERROR + "Error updating user " + VARIABLE + userId + ": " + VARIABLE + e.getMessage() + RESET);
                return jackson.writeValueAsString(Map.of("error", "User not found"));
            }
        });

        delete("/api/user/:id", (req, res) -> {
            String userId = req.params(":id");
            System.out.println(INFO + "[UserController] " + NEUTRAL + "DELETE request received for /api/user/" + VARIABLE + userId + RESET);
            dao.delete(userId);
            res.status(204);
            System.out.println(INFO + "[UserController] " + SUCCESS + "User " + VARIABLE + userId + SUCCESS + " deleted." + RESET);
            return "";
        });
    }
}