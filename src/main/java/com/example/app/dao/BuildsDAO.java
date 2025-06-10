// File: src/main/java/com/example/app/dao/BuildsDAO.java
package com.example.app.dao;

import com.example.app.model.Builds;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

/**
 * DAO for the Builds class, which manages collections of user-built pulseras.
 */
public class BuildsDAO {

    private final MongoCollection<Document> col;

    /**
     * Initializes the connection to the "builds" collection.
     * @param client The MongoClient instance.
     */
    public BuildsDAO(MongoClient client) {
        MongoDatabase db = client.getDatabase("myapp");
        col = db.getCollection("builds");
        System.out.println("Successfully initialized connection to 'builds' collection.");
    }

    /**
     * Converts a BSON Document to a Builds object.
     * @param d The Document to convert.
     * @return The corresponding Builds object.
     */
    private Builds docToBuilds(Document d) {
        Builds b = new Builds();
        b.setId(d.getObjectId("_id")); //
        b.setPulserasIds(d.getList("pulserasIds", ObjectId.class)); //
        return b;
    }

    /**
     * Creates a new builds collection in the database.
     * @param b The Builds object to create.
     */
    public void create(Builds b) {
        System.out.println("Trying to create new builds collection.");
        Document d = new Document()
                .append("pulserasIds", b.getPulserasIds()); //
        col.insertOne(d);
        // Set the generated ObjectId back into the object
        b.setId(d.getObjectId("_id")); //
        System.out.println("Builds collection created with ID: " + b.getId().toHexString());
    }

    /**
     * Finds a builds collection by its ObjectId.
     * @param id The ObjectId to search for.
     * @return An Optional containing the found Builds object, or empty if not found.
     */
    public Optional<Builds> findById(ObjectId id) {
        System.out.println("Searching for builds collection with ID: " + id.toHexString());
        Document d = col.find(eq("_id", id)).first();
        if (d == null) {
            System.out.println("Builds collection with ID " + id.toHexString() + " not found.");
            return Optional.empty();
        }
        System.out.println("Builds collection found by ID: " + id.toHexString());
        return Optional.of(docToBuilds(d));
    }

    /**
     * Updates an existing builds collection. This is typically used to add or remove
     * a pulseraId from the list.
     * @param b The Builds object with updated information.
     */
    public void update(Builds b) {
        System.out.println("Updating builds collection with ID: " + b.getId().toHexString());
        col.updateOne(eq("_id", b.getId()),
                new Document("$set", new Document("pulserasIds", b.getPulserasIds())) //
        );
        System.out.println("Builds collection with ID " + b.getId().toHexString() + " updated.");
    }

    /**
     * Deletes a builds collection by its ObjectId.
     * @param id The ObjectId of the collection to delete.
     */
    public void delete(ObjectId id) {
        System.out.println("Deleting builds collection with ID: " + id.toHexString());
        col.deleteOne(eq("_id", id));
        System.out.println("Builds collection with ID " + id.toHexString() + " deleted.");
    }
}