package com.example.app.dao;

import com.example.app.model.Favoritos;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import static com.mongodb.client.model.Updates.pull;
import static com.mongodb.client.model.Updates.push;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

public class FavoritosDAO {

    private final MongoCollection<Document> col;

    public FavoritosDAO(MongoClient client) {
        MongoDatabase db = client.getDatabase("myapp");
        col = db.getCollection("favoritos");
        System.out.println("Successfully initialized connection to 'favoritos' collection.");
    }

    private Favoritos docToFavoritos(Document d) {
        Favoritos f = new Favoritos();
        f.setId(d.getObjectId("_id"));
        f.setPulserasIds(d.getList("pulserasIds", ObjectId.class)); //
        return f;
    }

    public void create(Favoritos f) {
        Document d = new Document()
                .append("pulserasIds", f.getPulserasIds()); //
        col.insertOne(d);
        f.setId(d.getObjectId("_id"));
    }

    public Optional<Favoritos> findById(ObjectId id) {
        Document d = col.find(eq("_id", id)).first();
        return Optional.ofNullable(d).map(this::docToFavoritos);
    }

    public void update(Favoritos f) {
        col.updateOne(eq("_id", f.getId()),
                new Document("$set", new Document("pulserasIds", f.getPulserasIds()))
        );
    }

    public void addPulsera(ObjectId favoritosId, ObjectId pulserasId) {
        col.updateOne(eq("_id", favoritosId), push("pulserasId", pulserasId));
    }

    public void removePulsera(ObjectId favoritosId, ObjectId pulseraId) {
        col.updateOne(eq("_id", favoritosId), pull("pulserasIds", pulseraId));
    }
}
