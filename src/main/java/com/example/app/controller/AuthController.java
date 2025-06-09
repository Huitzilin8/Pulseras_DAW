package com.example.app.controller;

import com.example.app.dao.UsuarioDAO;
import com.example.app.model.User;
import static com.example.app.constants.ColorCodes.*;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.example.app.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import static spark.Spark.*;
import java.util.Map;
import java.util.Optional;

public class AuthController {
    private final UsuarioDAO dao;
    private static final ObjectMapper jackson = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public AuthController(UsuarioDAO dao) { this.dao = dao; }

    public void registerRoutes() {
        get("/login", (req, res) -> {
            System.out.println(INFO + "[AuthController] " + NEUTRAL + "GET request received for /login" + RESET);
            res.redirect("/login.html");
            return null;
        });

        post("/api/login", (req, res) -> {
            System.out.println(INFO + "[AuthController] " + NEUTRAL + "POST request received for /api/login" + RESET);
            try {
                User data = jackson.readValue(req.body(), User.class);
                System.out.println(INFO + "[AuthController] " + NEUTRAL + "Login attempt for: " + VARIABLE + data.getUsername() + RESET);

                Optional<Usuario> opt = dao.findByUsername(data.getUsername());
                if (!opt.isPresent()) {
                    System.out.println(ERROR + "[AuthController] " + NEUTRAL + "User not found: " + VARIABLE + data.getUsername() + RESET);
                    res.status(401);
                    return jackson.writeValueAsString(Map.of("success", false));
                }

                boolean passwordMatch = BCrypt.verifyer().verify(
                        data.getPasswordHash().toCharArray(),
                        opt.get().getHashContrasena()
                ).verified;

                if (passwordMatch) {
                    Usuario u = opt.get();
                    dao.update(u);
                    req.session().attribute("user", u);
                    res.status(200);
                    System.out.println(SUCCESS + "[AuthController] " + NEUTRAL + "User logged in successfully: " + VARIABLE + u.getNombreUsuario() + RESET);
                    return jackson.writeValueAsString(Map.of("success", true, "user", u));
                } else {
                    res.status(401);
                    System.out.println(ERROR + "[AuthController] " + NEUTRAL + "Invalid password for: " + VARIABLE + data.getUsername() + RESET);
                    return jackson.writeValueAsString(Map.of("success", false));
                }
            } catch (Exception e) {
                System.err.println(ERROR + "[AuthController] " + NEUTRAL + "Login error: " + e.getMessage() + RESET);
                e.printStackTrace();
                res.status(500);
                return jackson.writeValueAsString(Map.of("success", false, "error", "Internal server error"));
            }
        });

        post("/api/register", (req, res) -> {
            System.out.println(INFO + "[AuthController] " + NEUTRAL + "POST request received for /api/register" + RESET);
            try {
                Usuario data = jackson.readValue(req.body(), Usuario.class);
                String hash = BCrypt.withDefaults().hashToString(12, data.getHashContrasena().toCharArray());
                data.setHashContrasena(hash);
                data.setRol("user");
                dao.create(data);
                res.status(201);
                System.out.println(SUCCESS + "[AuthController] " + NEUTRAL + "New user registered: " + VARIABLE + data.getNombreUsuario() + RESET);
                return jackson.writeValueAsString(Map.of("success", true));
            } catch (Exception e) {
                res.status(400);
                return jackson.writeValueAsString(Map.of("success", false, "error", "Invalid registration data"));
            }
        });

        get("/logout", (req, res) -> {
            System.out.println(INFO + "[AuthController] " + NEUTRAL + "GET request received for /logout" + RESET);
            req.session().invalidate();
            res.redirect("/index.html");
            System.out.println(INFO + "[AuthController] " + NEUTRAL + "User logged out" + RESET);
            return null;
        });
    }
}