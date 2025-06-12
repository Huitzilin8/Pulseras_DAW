// File: src/main/java/com/example/app/App.java
package com.example.app;

import com.example.app.controller.*;
import com.example.app.dao.*;

import com.example.app.db.DBInit;


import static spark.Spark.*;

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
            System.out.println("[App.java] MongoDB connected successfully.");

            // Initialize DAO and routes
            UsuarioDAO userDao = new UsuarioDAO(client);
            FavoritosDAO favDao = new FavoritosDAO(client);
            BuildsDAO buildDao = new BuildsDAO(client);
            PulseraDAO pulseraDAO = new PulseraDAO(client);


            new AuthController(userDao).registerRoutes();
            new UsuarioController(userDao, favDao, buildDao, pulseraDAO).registerRoutes();

            MaterialDAO materialDao = new MaterialDAO(client);
            new MaterialController(materialDao).registerRoutes();

            new PulseraController(pulseraDAO, userDao, buildDao, materialDao).registerRoutes();

        } catch (MongoException e) {
            System.out.println("[App.java] DB connection failed: " + e.getMessage() );
            DBInit.createTestDatabase();  // This must not require a working client
        } catch (Exception e) {
            System.out.println("[App.java] Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}