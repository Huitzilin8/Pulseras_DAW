// File: src/main/java/dao/PulseraDAO.java
package com.example.app.dao;

import com.example.app.model.Pulsera;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class PulseraDAO {

    private final MongoCollection<Document> col;

    public PulseraDAO(MongoClient client) {
        MongoDatabase db = client.getDatabase("myapp");
        col = db.getCollection("pulseras");
        System.out.println("Successfully initialized connection to 'pulseras' collection.");
    }

    private Pulsera docToPulsera(Document d) {
        Pulsera p = new Pulsera();
        p.setId(d.getObjectId("_id"));
        p.setNombre(d.getString("nombre"));
        p.setDescripcion(d.getString("descripcion")); //
        p.setCircunferencia(d.getDouble("circunferencia")); //
        p.setPrecio(d.getDouble("precio")); //
        p.setMaterialesIds(d.getList("materialesIds", ObjectId.class)); //
        p.setColoresIds(d.getList("coloresIds", ObjectId.class)); //
        p.setDelisted(d.getBoolean("delisted")); //
        p.setUserBuilt(d.getBoolean("userBuilt")); //
        p.setImgURL(d.getString("imgURL"));
        return p;
    }

    public void create(Pulsera p) {
        Document d = new Document()
                .append("nombre", p.getNombre()) //
                .append("descripcion", p.getDescripcion()) //
                .append("circunferencia", p.getCircunferencia()) //
                .append("precio", p.getPrecio()) //
                .append("materialesIds", p.getMaterialesIds()) //
                .append("coloresIds", p.getColoresIds()) //
                .append("delisted", p.getDelisted()) //
                .append("userBuilt", p.getUserBuilt())
                .append("imgURL", p.getImgURL());
        col.insertOne(d);
        p.setId(d.getObjectId("_id"));
    }

    public Optional<Pulsera> findById(ObjectId id) {
        Document d = col.find(eq("_id", id)).first();
        return Optional.ofNullable(d).map(this::docToPulsera);
    }

    public List<Pulsera> findAvailable() {
        List<Pulsera> list = new ArrayList<>();
        col.find(eq("delisted", false)).forEach(d -> list.add(docToPulsera(d)));
        return list;
    }

    public List<Pulsera> listAll() {
        List<Pulsera> list = new ArrayList<>();
        col.find().forEach(d -> list.add(docToPulsera(d)));
        return list;
    }

    public void update(Pulsera p) {
        col.updateOne(eq("_id", p.getId()),
                new Document("$set", new Document()
                        .append("descripcion", p.getDescripcion())
                        .append("circunferencia", p.getCircunferencia())
                        .append("precio", p.getPrecio())
                        .append("materialesIds", p.getMaterialesIds())
                        .append("coloresIds", p.getColoresIds())
                        .append("delisted", p.getDelisted())
                        .append("userBuilt", p.getUserBuilt())
                )
        );
    }

    public List<Pulsera> findByIds(List<ObjectId> ids) {
        List<Pulsera> list = new ArrayList<>();
        col.find(eq("_id", ids)).forEach(d -> list.add(docToPulsera(d)));
        return list;
    }

    /*
    public List<Pulsera> findAvailableByMaxPrice(double maxPrice) {
        List<Pulsera> list = new ArrayList<>();
        col.find(eq("delisted", false)).forEach(d -> list.add(docToPulsera(d)));
        list.sort();
        return list;
    };
    */

    public void setDelisted(ObjectId id, boolean delisted) {
        try {
            Optional<Pulsera> p = findById(id);
            p.ifPresent(pulsera -> pulsera.setDelisted(delisted));
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete(ObjectId id) {
        col.deleteOne(eq("_id", id));
    }
}
