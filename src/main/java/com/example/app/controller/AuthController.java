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

        get("/admin", (req, res) -> {
            // Panel de administración, pendiente logica autenticación
            System.out.println(INFO + "[AuthController] " + NEUTRAL + "GET request received for /admin" + RESET);
            res.redirect("/admin.html");
            return null;
        });

        get("/account", (req, res) -> {
            System.out.println(INFO + "[AuthController] " + NEUTRAL + "GET request received for /account" + RESET);
            try {
                // Retrieve the user from the session
                // Assuming your 'user' attribute in the session stores a Usuario object
                Usuario currentUser = req.session().attribute("usuario");

                if (currentUser != null) {
                    // User is authenticated.
                    // You can optionally return JSON with user data if needed for a SPA
                    // or just redirect as you were doing.
                    // For now, let's keep the redirect to account.html
                    System.out.println(INFO + "[AuthController] " + NEUTRAL + "User '" + VARIABLE + currentUser.getNombreUsuario() + NEUTRAL + "' is authenticated. Redirecting to /account.html" + RESET);
                    res.redirect("/account.html");
                    return null; // Important: return null after redirect to stop further processing in Spark
                } else {
                    // User is NOT authenticated.
                    System.out.println(ERROR + "[AuthController] " + NEUTRAL + "Unauthenticated access to /account. Redirecting to /login.html" + RESET);
                    // Option 1: Redirect to login page
                    res.redirect("/login.html");
                    return null; // Important: return null after redirect

                /*
                // Option 2: Return JSON response indicating unauthenticated status
                // This would be more suitable for an API endpoint if the frontend
                // handles the redirection based on the JSON response.
                res.status(401); // Unauthorized
                return jackson.writeValueAsString(Map.of(
                    "success", false,
                    "message", "Unauthorized access. Please log in.",
                    "redirect", "/login.html" // Hint for frontend to redirect
                ));
                */
                }
            } catch (Exception e) {
                System.err.println(ERROR + "[AuthController] Error processing /account request: " + e.getMessage() + RESET);
                e.printStackTrace(); // Log the stack trace for debugging

                // Return a 500 Internal Server Error if an unexpected error occurs
                res.status(500);
                return jackson.writeValueAsString(Map.of(
                        "success", false,
                        "message", "Internal server error while processing account request."
                ));
            }
        });

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

        get("/logout", (req, res) -> {
            System.out.println(INFO + "[AuthController] " + NEUTRAL + "GET request received for /logout" + RESET);
            req.session().invalidate();
            res.redirect("/index.html");
            System.out.println(INFO + "[AuthController] " + NEUTRAL + "User logged out" + RESET);
            return null;
        });

        // En AuthController.java
        get("/api/auth/status", (req, res) -> {
            try {
                Usuario usuario = req.session().attribute("usuario");
                if (usuario != null) {
                    return jackson.writeValueAsString(Map.of(
                            "authenticated", true,
                            "username", usuario.getNombreUsuario(),
                            "role", usuario.getRol()
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
                Optional<Usuario> opt = dao.findByEmail(email); // Assuming findByUsername now searches by email
                if (opt.isEmpty()) {
                    System.out.println(ERROR + "[AuthController] " + NEUTRAL + "User not found with email: " + VARIABLE + email + RESET);
                    res.status(401);
                    return jackson.writeValueAsString(Map.of("success", false, "message", "Invalid credentials"));
                }

                Usuario usuario = opt.get();
                boolean passwordMatch = BCrypt.verifyer().verify(
                        password.toCharArray(),
                        usuario.getHashContrasena()
                ).verified;

                if (passwordMatch) {
                    dao.update(usuario); // Consider if you need to update anything on login (e.g., last login time)
                    req.session().attribute("usuario", usuario);
                    res.status(200);
                    System.out.println(SUCCESS + "[AuthController] " + NEUTRAL + "User logged in successfully: " + VARIABLE + usuario.getNombreUsuario() + RESET);
                    return jackson.writeValueAsString(Map.of("success", true, "user", usuario));
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
    }
}