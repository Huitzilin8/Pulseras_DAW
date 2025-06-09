package com.example.app.dao;

import com.example.app.model.Sesion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

public class SesionDAO {

    private final MongoCollection<Document> col;

    public SesionDAO(MongoClient client) {
        MongoDatabase db = client.getDatabase("myapp");
        col = db.getCollection("sesiones");
        System.out.println("Successfully initialized connection to 'sesiones' collection.");
    }

    private Sesion docToSesion(Document d) {
        Sesion s = new Sesion();
        s.setSessionId(d.getString("_id")); // Using _id as sessionId
        s.setUsuarioId(d.getObjectId("usuarioId")); //
        s.setFechaCreacion(d.getDate("fechaCreacion")); //
        s.setUltimaActividad(d.getDate("ultimaActividad")); //
        s.setChatId(d.getObjectId("chatId")); //
        return s;
    }

    public void create(Sesion s) {
        Document d = new Document()
                .append("_id", s.getSessionId()) // Using sessionId as the document's primary key
                .append("usuarioId", s.getUsuarioId()) //
                .append("fechaCreacion", s.getFechaCreacion()) //
                .append("ultimaActividad", s.getUltimaActividad()) //
                .append("chatId", s.getChatId()); //
        col.insertOne(d);
    }

    public Optional<Sesion> findById(String sessionId) {
        Document d = col.find(eq("_id", sessionId)).first();
        return Optional.ofNullable(d).map(this::docToSesion);
    }

    public void update(Sesion s) {
        col.updateOne(eq("_id", s.getSessionId()),
                new Document("$set", new Document()
                        .append("usuarioId", s.getUsuarioId())
                        .append("ultimaActividad", s.getUltimaActividad())
                        .append("chatId", s.getChatId())
                )
        );
    }

    public void delete(String sessionId) {
        col.deleteOne(eq("_id", sessionId));
    }
}