// File: src/main/java/com/example/app/dao/MaterialDAO.java
package com.example.app.dao;

import com.example.app.model.Material;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

/**
 * DAO for the Material class, handling all database operations.
 */
public class MaterialDAO {

    private final MongoCollection<Document> col;

    /**
     * Initializes the connection to the "materiales" collection.
     * @param client The MongoClient instance.
     */
    public MaterialDAO(MongoClient client) {
        MongoDatabase db = client.getDatabase("myapp"); // Assumes the same database "myapp"
        col = db.getCollection("materiales"); // Collection for Material objects
        System.out.println("[MaterialDAO] [MaterialDAO] Successfully initialized connection to 'materiales' collection.");
    }

    /**
     * Converts a BSON Document to a Material object.
     * @param d The Document to convert.
     * @return The corresponding Material object.
     */
    private Material docToMaterial(Document d) {
        Material m = new Material();
        m.setId(d.getObjectId("_id")); //
        m.setNombre(d.getString("nombre")); //
        m.setDescripcion(d.getString("descripcion")); //
        m.setTipo(d.getString("tipo")); //
        m.setColorId(d.getObjectId("colorId")); //
        m.setTamanoMm(d.getInteger("tamanoMm", 0)); //
        m.setCantidadInventario(d.getInteger("cantidadInventario", 0)); //
        m.setRutaImagen(d.getString("rutaImagen")); //
        return m;
    }

    /**
     * Creates a new material in the database.
     * @param m The Material object to create.
     */
    public void create(Material m) {
        System.out.println("[MaterialDAO] Trying to create new material: " + m.getNombre());
        Document d = new Document()
                .append("nombre", m.getNombre()) //
                .append("descripcion", m.getDescripcion()) //
                .append("tipo", m.getTipo()) //
                .append("colorId", m.getColorId()) //
                .append("tamanoMm", m.getTamanoMm()) //
                .append("cantidadInventario", m.getCantidadInventario()) //
                .append("rutaImagen", m.getRutaImagen()); //
        col.insertOne(d);
        // Set the generated ObjectId back into the material object
        m.setId(d.getObjectId("_id"));
        System.out.println("[MaterialDAO] Material created: " + m.getNombre() + " (ID: " + m.getId().toHexString() + ")");
    }

    /**
     * Finds a material by its ObjectId.
     * @param id The ObjectId to search for.
     * @return An Optional containing the found Material, or empty if not found.
     */
    public Optional<Material> findById(ObjectId id) {
        System.out.println("[MaterialDAO] Searching for material with ID: " + id.toHexString());
        Document d = col.find(eq("_id", id)).first();
        if (d == null) {
            System.out.println("[MaterialDAO] Material with ID " + id.toHexString() + " not found.");
            return Optional.empty();
        }
        Material material = docToMaterial(d);
        System.out.println("[MaterialDAO] Material found by ID: " + id.toHexString() + " (Name: " + material.getNombre() + ")");
        return Optional.of(material);
    }

    /**
     * Retrieves all materials from the collection.
     * @return A List of all Material objects.
     */
    public List<Material> listAll() {
        System.out.println("[MaterialDAO] Listing all materials.");
        List<Material> list = new ArrayList<>();
        col.find().forEach(d -> list.add(docToMaterial(d)));
        System.out.println("[MaterialDAO] Retrieved " + list.size() + " materials.");
        return list;
    }

    /**
     * Updates an existing material's information.
     * @param m The Material object with updated information.
     */
    public void update(Material m) {
        System.out.println("[MaterialDAO] Updating material with ID: " + m.getId().toHexString());
        col.updateOne(eq("_id", m.getId()),
                new Document("$set", new Document()
                        .append("nombre", m.getNombre())
                        .append("descripcion", m.getDescripcion())
                        .append("tipo", m.getTipo())
                        .append("colorId", m.getColorId())
                        .append("tamanoMm", m.getTamanoMm())
                        .append("cantidadInventario", m.getCantidadInventario())
                        .append("rutaImagen", m.getRutaImagen())
                )
        );
        System.out.println("[MaterialDAO] Material with ID " + m.getId().toHexString() + " updated.");
    }

    /**
     * Deletes a material by its ObjectId.
     * @param id The ObjectId of the material to delete.
     */
    public void delete(ObjectId id) {
        System.out.println("[MaterialDAO] Deleting material with ID: " + id.toHexString());
        col.deleteOne(eq("_id", id));
        System.out.println("[MaterialDAO] Material with ID " + id.toHexString() + " deleted.");
    }
}