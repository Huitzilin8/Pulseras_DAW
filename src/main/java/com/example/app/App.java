// File: src/main/java/com/example/app/App.java
package com.example.app;

import com.example.app.controller.UsuarioController;
import com.example.app.dao.UsuarioDAO;
import static com.example.app.constants.ColorCodes.*;
import com.example.app.controller.AuthController;
import com.example.app.controller.UserController;
import com.example.app.db.DBInit;


import static spark.Spark.*;

import com.example.app.model.Usuario;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class App {
    public static void main(String[] args) {
        // Setup server
        port(4567);
        staticFiles.location("/public");

        // Mongo connection
        MongoClient client = null;
        try {
            client = MongoClients.create("mongodb+srv://emiliocastillon8:b1bV10jueIac55QE@cluster0.ngkrl.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0");
            MongoDatabase db = client.getDatabase("myapp");

            // Test connection
            db.listCollectionNames().first(); // Will throw if fails

            // If we got here, connection is fine
            System.out.println(INFO + "[App.java] " + SUCCESS + "MongoDB connected successfully." + RESET);

            // Initialize DAO and routes
            UsuarioDAO userDao = new UsuarioDAO(client);
            new AuthController(userDao).registerRoutes();
            new UsuarioController(userDao).registerRoutes();

        } catch (MongoException e) {
            System.out.println(INFO + "[App.java] " + ERROR + "DB connection failed: " + VARIABLE + e.getMessage() + RESET);
            DBInit.createTestDatabase();  // This must not require a working client
        } catch (Exception e) {
            System.out.println(INFO + "[App.java] " + ERROR + "Unexpected error: " + e.getMessage() + RESET);
            e.printStackTrace();
        }
    }
}