package com.example.app.dao;


import com.example.app.model.Usuario;

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
 * DAO for the Usuario class, handling all database operations.
 */
public class UsuarioDAO {
    private final MongoCollection<Document> col;

    /**
     * Initializes the connection to the "usuarios" collection.
     * @param client The MongoClient instance.
     */
    public UsuarioDAO(MongoClient client) {
        MongoDatabase db = client.getDatabase("myapp"); // Assuming the same database "myapp"
        col = db.getCollection("usuarios"); // Collection for Usuario objects
        System.out.println("Successfully initialized connection to 'usuarios' collection.");
    }

    /**
     * Converts a BSON Document to a Usuario object.
     * @param d The Document to convert.
     * @return The corresponding Usuario object.
     */
    private Usuario docToUsuario(Document d) {
        Usuario u = new Usuario();
        u.setId(d.getObjectId("_id"));
        u.setNombreUsuario(d.getString("nombreUsuario"));
        u.setHashContrasena(d.getString("hashContrasena"));
        u.setCorreo(d.getString("correo"));
        u.setRol(d.getString("rol"));
        u.setFavoritosId(d.getObjectId("favoritosId"));
        u.setBuildsId(d.getObjectId("buildsId"));
        return u;
    }

    /**
     * Creates a new user in the database.
     * @param u The Usuario object to create.
     */
    public void create(Usuario u) {
        System.out.println("Trying to create new user: " + u.getNombreUsuario());
        Document d = new Document()
                .append("nombreUsuario", u.getNombreUsuario())
                .append("hashContrasena", u.getHashContrasena())
                .append("correo", u.getCorreo())
                .append("rol", u.getRol())
                .append("favoritosId", u.getFavoritosId())
                .append("buildsId", u.getBuildsId());
        col.insertOne(d);
        // Set the generated ObjectId back into the user object
        u.setId(d.getObjectId("_id"));
        System.out.println("User created: " + u.getNombreUsuario() + " (ID: " + u.getId().toHexString() + ")");
    }

    /**
     * Finds a user by their username.
     * @param nombreUsuario The username to search for.
     * @return An Optional containing the found Usuario, or empty if not found.
     */
    public Optional<Usuario> findByUsername(String nombreUsuario) {
        System.out.println("Searching for user with username: " + nombreUsuario);
        Document d = col.find(eq("nombreUsuario", nombreUsuario)).first();
        if (d == null) {
            System.out.println("User with username " + nombreUsuario + " not found.");
            return Optional.empty();
        }
        Usuario usuario = docToUsuario(d);
        System.out.println("User found by username: " + nombreUsuario + " (ID: " + usuario.getId().toHexString() + ")");
        return Optional.of(usuario);
    }

    /**
     * Finds a user by their ObjectId.
     * @param id The ObjectId to search for.
     * @return An Optional containing the found Usuario, or empty if not found.
     */
    public Optional<Usuario> findById(ObjectId id) {
        System.out.println("Searching for user with ID: " + id.toHexString());
        Document d = col.find(eq("_id", id)).first();
        if (d == null) {
            System.out.println("User with ID " + id.toHexString() + " not found.");
            return Optional.empty();
        }
        Usuario usuario = docToUsuario(d);
        System.out.println("User found by ID: " + id.toHexString() + " (Username: " + usuario.getNombreUsuario() + ")");
        return Optional.of(usuario);
    }

    /**
     * Retrieves all users from the collection.
     * @return A List of all Usuario objects.
     */
    public List<Usuario> listAll() {
        System.out.println("Listing all users.");
        List<Usuario> list = new ArrayList<>();
        col.find().forEach(d -> list.add(docToUsuario(d)));
        System.out.println("Retrieved " + list.size() + " users.");
        return list;
    }

    /**
     * Updates an existing user's information.
     * @param u The Usuario object with updated information.
     */
    public void update(Usuario u) {
        System.out.println("Updating user with ID: " + u.getId().toHexString());
        col.updateOne(eq("_id", u.getId()),
                new Document("$set", new Document()
                        .append("nombreUsuario", u.getNombreUsuario())
                        .append("hashContrasena", u.getHashContrasena())
                        .append("correo", u.getCorreo())
                        .append("rol", u.getRol())
                        .append("favoritosId", u.getFavoritosId())
                        .append("buildsId", u.getBuildsId())
                )
        );
        System.out.println("User with ID " + u.getId().toHexString() + " updated.");
    }

    /**
     * Deletes a user by their ObjectId.
     * @param id The ObjectId of the user to delete.
     */
    public void delete(ObjectId id) {
        System.out.println("Deleting user with ID: " + id.toHexString());
        col.deleteOne(eq("_id", id));
        System.out.println("User with ID " + id.toHexString() + " deleted.");
    }
}