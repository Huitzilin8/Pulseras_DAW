// File: src/main/java/com/example/app/db/DBInit.java
package com.example.app.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;

import static com.example.app.constants.ColorCodes.*;

import org.bson.Document;

import java.util.Arrays;

public class DBInit {

    public static void createTestDatabase() {
        System.out.println(INFO + "Creating test database with sample data..." + RESET);

        // Use localhost or fallback test URI
        try (MongoClient client = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase db = client.getDatabase("myapp");

            // Drop if exists (optional)
            db.drop();

            // Create collections and insert test data
            MongoCollection<Document> users = db.getCollection("users");
            users.insertMany(Arrays.asList(
                    new Document("username", "admin")
                            .append("password", "$2a$10$XQe/NA8z5o9L7.qYZKvSMevP8OzMOM9BzEyLD/rKTXQ61muas49Ra") // hash of "admin"
                            .append("email", "admin@example.com")
                            .append("role", "admin"),
                    new Document("username", "test user")
                            .append("password", "$2a$10$VXoStiZAw8uL9/IGK7S7E.2r4E9yK9T6NdCPbn3C9AAsgPdRBFg9q") // hash of "test123"
                            .append("email", "user@example.com")
                            .append("role", "user")
            ));

            System.out.println(SUCCESS + "Test database created successfully." + RESET);

        } catch (Exception e) {
            System.out.println(ERROR + "Failed to create test database: " + VARIABLE + e.getMessage() + RESET);
            e.printStackTrace();
        }
    }
}
