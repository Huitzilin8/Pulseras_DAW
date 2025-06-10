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

        get("/register", (req, res) -> {
            System.out.println(INFO + "[AuthController] " + NEUTRAL + "GET request received for /register" + RESET);
            res.redirect("/register.html");
            return null;
        });

        // En AuthController.java
        get("/api/auth/status", (req, res) -> {
            try {
                Usuario user = req.session().attribute("user");
                if (user != null) {
                    return jackson.writeValueAsString(Map.of(
                            "authenticated", true,
                            "username", user.getNombreUsuario()
                    ));
                } else {
                    return jackson.writeValueAsString(Map.of(
                            "authenticated", false
                    ));
                }
            } catch (Exception e) {
                res.status(500);
                return jackson.writeValueAsString(Map.of(
                        "authenticated", false,
                        "error", "Error checking auth status"
                ));
            }
        });

        post("/api/auth/login", (req, res) -> {
            System.out.println(INFO + "[AuthController] " + NEUTRAL + "POST request received for /api/auth/login" + RESET);
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

        post("/api/auth/register", (req, res) -> {
            System.out.println(INFO + "[AuthController] " + NEUTRAL + "POST request received for /api/auth/register" + RESET);
            try {
                // Leer como Map en lugar de Usuario para mayor flexibilidad
                Map<String, String> data = jackson.readValue(req.body(), Map.class);

                // Validar campos requeridos
                if (!data.containsKey("email") || !data.containsKey("password") ||
                        !data.containsKey("nombre") || !data.containsKey("apellido")) {
                    res.status(400);
                    return jackson.writeValueAsString(Map.of(
                            "success", false,
                            "error", "Todos los campos son requeridos"
                    ));
                }

                // Verificar si el email ya existe
                if (dao.findByEmail(data.get("email")).isPresent()) {
                    res.status(400);
                    return jackson.writeValueAsString(Map.of(
                            "success", false,
                            "error", "El email ya está registrado"
                    ));
                }

                // Crear nuevo usuario
                Usuario nuevoUsuario = new Usuario();
                nuevoUsuario.setNombreUsuario(data.get("nombre"));
                nuevoUsuario.setCorreo(data.get("email"));

                // Generar hash de la contraseña
                String hash = BCrypt.withDefaults().hashToString(12, data.get("password").toCharArray());
                nuevoUsuario.setHashContrasena(hash);
                nuevoUsuario.setRol("user");

                // Establecer nombreUsuario (podría ser el email o generarlo)
                nuevoUsuario.setNombreUsuario(data.get("email"));

                dao.create(nuevoUsuario);
                res.status(201);
                System.out.println(SUCCESS + "[AuthController] " + NEUTRAL + "New user registered: " + VARIABLE + nuevoUsuario.getEmail() + RESET);
                return jackson.writeValueAsString(Map.of("success", true));

            } catch (Exception e) {
                System.out.println(ERROR + "[AuthController] User failed to register: " + e.getMessage() + RESET);
                e.printStackTrace();
                res.status(400);
                return jackson.writeValueAsString(Map.of(
                        "success", false,
                        "error", e.getMessage() // Devuelve el mensaje de error específico
                ));
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