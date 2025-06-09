package com.example.app.dao;

import com.example.app.model.Color;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

public class ColorDAO {

    private final MongoCollection<Document> col;

    public ColorDAO(MongoClient client) {
        MongoDatabase db = client.getDatabase("myapp");
        col = db.getCollection("colores");
        System.out.println("Successfully initialized connection to 'colores' collection.");
    }

    private Color docToColor(Document d) {
        Color c = new Color();
        c.setId(d.getObjectId("_id"));
        c.setNombre(d.getString("nombre")); //
        c.setCodigoHex(d.getString("codigoHex")); //
        return c;
    }

    public void create(Color c) {
        Document d = new Document()
                .append("nombre", c.getNombre()) //
                .append("codigoHex", c.getCodigoHex()); //
        col.insertOne(d);
        c.setId(d.getObjectId("_id"));
    }

    public Optional<Color> findById(ObjectId id) {
        Document d = col.find(eq("_id", id)).first();
        return Optional.ofNullable(d).map(this::docToColor);
    }

    public List<Color> listAll() {
        List<Color> list = new ArrayList<>();
        col.find().forEach(d -> list.add(docToColor(d)));
        return list;
    }

    public void update(Color c) {
        col.updateOne(eq("_id", c.getId()),
                new Document("$set", new Document()
                        .append("nombre", c.getNombre())
                        .append("codigoHex", c.getCodigoHex())
                )
        );
    }

    public void delete(ObjectId id) {
        col.deleteOne(eq("_id", id));
    }
}