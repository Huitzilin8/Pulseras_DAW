package com.example.app.dao;

import com.example.app.model.Mensaje;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

public class MensajeDAO {

    private final MongoCollection<Document> col;

    public MensajeDAO(MongoClient client) {
        MongoDatabase db = client.getDatabase("myapp");
        col = db.getCollection("mensajes");
        System.out.println("Successfully initialized connection to 'mensajes' collection.");
    }

    private Mensaje docToMensaje(Document d) {
        Mensaje m = new Mensaje();
        m.setId(d.getObjectId("_id"));
        m.setContenido(d.getString("contenido")); //
        m.setFecha(d.getDate("fecha")); //
        m.setSessionId(d.getString("sessionId")); //
        m.setRemitenteId(d.getObjectId("remitenteId")); //
        m.setEsAdmin(d.getBoolean("esAdmin", false)); //
        return m;
    }

    public void create(Mensaje m) {
        Document d = new Document()
                .append("contenido", m.getContenido()) //
                .append("fecha", m.getFecha()) //
                .append("sessionId", m.getSessionId()) //
                .append("remitenteId", m.getRemitenteId()) //
                .append("esAdmin", m.isEsAdmin()); //
        col.insertOne(d);
        m.setId(d.getObjectId("_id"));
    }

    public List<Mensaje> findBySessionId(String sessionId) {
        List<Mensaje> list = new ArrayList<>();
        col.find(eq("sessionId", sessionId))
                .sort(Sorts.ascending("fecha"))
                .forEach(d -> list.add(docToMensaje(d)));
        return list;
    }
}
