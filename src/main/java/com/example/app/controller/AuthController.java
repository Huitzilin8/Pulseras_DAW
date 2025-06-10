package com.example.app.controller;

import com.example.app.dao.UsuarioDAO;
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
                // Read the request body as a Map to handle "email" directly
                Map<String, String> loginData = jackson.readValue(req.body(), Map.class);

                // Validate required fields
                if (!loginData.containsKey("email") || !loginData.containsKey("password")) {
                    res.status(400);
                    return jackson.writeValueAsString(Map.of(
                            "success", false,
                            "message", "Email and password are required"
                    ));
                }

                String email = loginData.get("email");
                String password = loginData.get("password");

                System.out.println(INFO + "[AuthController] " + NEUTRAL + "Login attempt for email: " + VARIABLE + email + RESET);

                // Find by username, which is stored as the email from registration
                Optional<Usuario> opt = dao.findByUsername(email); // Assuming findByUsername now searches by email
                if (opt.isEmpty()) {
                    System.out.println(ERROR + "[AuthController] " + NEUTRAL + "User not found with email: " + VARIABLE + email + RESET);
                    res.status(401);
                    return jackson.writeValueAsString(Map.of("success", false, "message", "Invalid credentials"));
                }

                Usuario user = opt.get();
                boolean passwordMatch = BCrypt.verifyer().verify(
                        password.toCharArray(),
                        user.getHashContrasena()
                ).verified;

                if (passwordMatch) {
                    dao.update(user); // Consider if you need to update anything on login (e.g., last login time)
                    req.session().attribute("user", user);
                    res.status(200);
                    System.out.println(SUCCESS + "[AuthController] " + NEUTRAL + "User logged in successfully: " + VARIABLE + user.getNombreUsuario() + RESET);
                    return jackson.writeValueAsString(Map.of("success", true, "user", user));
                } else {
                    res.status(401);
                    System.out.println(ERROR + "[AuthController] " + NEUTRAL + "Invalid password for email: " + VARIABLE + email + RESET);
                    return jackson.writeValueAsString(Map.of("success", false, "message", "Invalid credentials"));
                }
            } catch (Exception e) {
                System.err.println(ERROR + "[AuthController] " + NEUTRAL + "Login error: " + e.getMessage() + RESET);
                e.printStackTrace();
                res.status(500);
                return jackson.writeValueAsString(Map.of("success", false, "message", "Internal server error"));
            }
        });

        post("/api/auth/register", (req, res) -> {
            System.out.println(INFO + "[AuthController] " + NEUTRAL + "POST request received for /api/auth/register" + RESET);
            try {
                // Leer como Map en lugar de Usuario para mayor flexibilidad
                Map<String, String> data = jackson.readValue(req.body(), Map.class);

                // Validar campos requeridos
                if (!data.containsKey("email") || !data.containsKey("password") ||
                        !data.containsKey("nombre")) {
                    res.status(400);
                    return jackson.writeValueAsString(Map.of(
                            "success", false,
                            "error", "Todos los campos son requeridos"
                    ));
                }

                // Verificar si el correo ya está en uso
                if (dao.checkEmailUse(data.get("email"))) {
                    res.status(400);
                    return jackson.writeValueAsString(Map.of(
                            "success", false,
                            "error", "El correo electrónico ya está registrado"
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

                dao.create(nuevoUsuario);
                res.status(201);
                System.out.println(SUCCESS + "[AuthController] " + NEUTRAL + "New user registered: " + VARIABLE + nuevoUsuario.getCorreo() + RESET);
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