package com.example.app.dao;

import com.example.app.model.Ventas;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

public class VentasDAO {

    private final MongoCollection<Document> col;

    public VentasDAO(MongoClient client) {
        MongoDatabase db = client.getDatabase("myapp");
        col = db.getCollection("ventas");
        System.out.println("Successfully initialized connection to 'ventas' collection.");
    }

    private Ventas docToVentas(Document d) {
        Ventas v = new Ventas();
        v.setId(d.getObjectId("_id"));
        v.setUsuarioId(d.getObjectId("usuarioId")); //
        v.setPulseraId(d.getObjectId("pulseraId")); //
        return v;
    }

    public void create(Ventas v) {
        Document d = new Document()
                .append("usuarioId", v.getUsuarioId()) //
                .append("pulseraId", v.getPulseraId()); //
        col.insertOne(d);
        v.setId(d.getObjectId("_id"));
    }

    public Optional<Ventas> findById(ObjectId id) {
        Document d = col.find(eq("_id", id)).first();
        return Optional.ofNullable(d).map(this::docToVentas);
    }

    public List<Ventas> findByUsuarioId(ObjectId usuarioId) {
        List<Ventas> list = new ArrayList<>();
        col.find(eq("usuarioId", usuarioId)).forEach(d -> list.add(docToVentas(d)));
        return list;
    }

    public List<Ventas> listAll() {
        List<Ventas> list = new ArrayList<>();
        col.find().forEach(d -> list.add(docToVentas(d)));
        return list;
    }

    public void delete(ObjectId id) {
        col.deleteOne(eq("_id", id));
    }
}