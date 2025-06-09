// File: src/main/java/com/example/app/dao/UserDAO.java
package com.example.app.dao;

import com.example.app.model.User;
import static com.example.app.constants.ColorCodes.*; // Import your color codes

import com.mongodb.client.*;
import org.bson.Document;
import static com.mongodb.client.model.Filters.*;
import java.time.Instant;
import java.util.*;

public class UserDAO {
    private final MongoCollection<Document> col;
    public UserDAO(MongoClient client) {
        MongoDatabase db = client.getDatabase("myapp");
        col = db.getCollection("users");
        System.out.println(INFO + "[UserDAO] " + SUCCESS + "Successfully initialized connection to 'users' collection." + RESET);
    }
    public void create(User u) {
        System.out.println(INFO + "[UserDAO] " + NEUTRAL + "Trying to create new user" + RESET);
        Document d = new Document()
                .append("username", u.getUsername())
                .append("passwordHash", u.getPasswordHash())
                .append("role", u.getRole())
                .append("registeredAt", u.getRegisteredAt().toString())
                .append("lastLogin", u.getLastLogin().toString());
        col.insertOne(d);
            u.setId(d.getObjectId("_id").toHexString());
        System.out.println(INFO + "[UserDAO] " + SUCCESS + "User created: " + VARIABLE + u.getUsername() + " (ID: " + u.getId() + ")" + RESET);
    }
    public Optional<User> findByUsername(String username) {
        System.out.println(INFO + "[UserDAO] " + NEUTRAL + "Searching for user with username: " + VARIABLE + username + RESET);
        Document d = col.find(eq("username", username)).first();
        if (d == null) {
            System.out.println(INFO + "[UserDAO] " + NEUTRAL + "User with username " + VARIABLE + username + NEUTRAL + " not found." + RESET);
            return Optional.empty();
        }
        User user = docToUser(d);
        System.out.println(INFO + "[UserDAO] " + SUCCESS + "User found by username: " + VARIABLE + username + " (ID: " + user.getId() + ")" + RESET);
        return Optional.of(user);
    }
    public Optional<User> findById(String id) {
        System.out.println(INFO + "[UserDAO] " + NEUTRAL + "Searching for user with ID: " + VARIABLE + id + RESET);
        Document d = col.find(eq("_id", new org.bson.types.ObjectId(id))).first();
        if (d == null) {
            System.out.println(INFO + "[UserDAO] " + NEUTRAL + "User with ID " + VARIABLE + id + NEUTRAL + " not found." + RESET);
            return Optional.empty();
        }
        User user = docToUser(d);
        System.out.println(INFO + "[UserDAO] " + SUCCESS + "User found by ID: " + VARIABLE + id + " (Username: " + user.getUsername() + ")" + RESET);
        return Optional.of(user);
    }
    public List<User> listAll() {
        System.out.println(INFO + "[UserDAO] " + NEUTRAL + "Listing all users." + RESET);
        List<User> list = new ArrayList<>();
        col.find().forEach(d -> list.add(docToUser(d)));
        System.out.println(INFO + "[UserDAO] " + SUCCESS + "Retrieved " + VARIABLE + list.size() + SUCCESS + " users." + RESET);
        return list;
    }
    public void update(User u) {
        System.out.println(INFO + "[UserDAO] " + NEUTRAL + "Updating user with ID: " + VARIABLE + u.getId() + RESET);
        col.updateOne(eq("_id", new org.bson.types.ObjectId(u.getId())),
                new Document("$set", new Document()
                        .append("username", u.getUsername())
                        .append("role", u.getRole()))
        );
        System.out.println(INFO + "[UserDAO] " + SUCCESS + "User with ID " + VARIABLE + u.getId() + SUCCESS + " updated." + RESET);
    }
    public void delete(String id) {
        System.out.println(INFO + "[UserDAO] " + NEUTRAL + "Deleting user with ID: " + VARIABLE + id + RESET);
        col.deleteOne(eq("_id", new org.bson.types.ObjectId(id)));
        System.out.println(INFO + "[UserDAO] " + SUCCESS + "User with ID " + VARIABLE + id + SUCCESS + " deleted." + RESET);
    }
    private User docToUser(Document d) {
        User u = new User();
        u.setId(d.getObjectId("_id").toHexString());
        u.setUsername(d.getString("username"));
        u.setPasswordHash(d.getString("passwordHash"));
        u.setRole(d.getString("role"));
        u.setRegisteredAt(Instant.parse(d.getString("registeredAt")));
        u.setLastLogin(Instant.parse(d.getString("lastLogin")));
        return u;
    }
}