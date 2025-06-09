package com.example.app.dao;

import com.example.app.model.Chat;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

public class ChatDAO {

    private final MongoCollection<Document> col;

    public ChatDAO(MongoClient client) {
        MongoDatabase db = client.getDatabase("myapp");
        col = db.getCollection("chats");
        System.out.println("Successfully initialized connection to 'chats' collection.");
    }

    private Chat docToChat(Document d) {
        Chat c = new Chat();
        c.setId(d.getObjectId("_id"));
        c.setSessionId(d.getString("sessionId")); //
        c.setActivo(d.getBoolean("activo")); //
        c.setFechaUltimoMensaje(d.getDate("fechaUltimoMensaje")); //
        return c;
    }

    public void create(Chat c) {
        Document d = new Document()
                .append("sessionId", c.getSessionId()) //
                .append("activo", c.getActivo()) //
                .append("fechaUltimoMensaje", c.getFechaUltimoMensaje()); //
        col.insertOne(d);
        c.setId(d.getObjectId("_id"));
    }

    public Optional<Chat> findById(ObjectId id) {
        Document d = col.find(eq("_id", id)).first();
        return Optional.ofNullable(d).map(this::docToChat);
    }

    public Optional<Chat> findBySessionId(String sessionId) {
        Document d = col.find(eq("sessionId", sessionId)).first();
        return Optional.ofNullable(d).map(this::docToChat);
    }

    public List<Chat> findActiveChats() {
        List<Chat> list = new ArrayList<>();
        col.find(eq("activo", true)).forEach(d -> list.add(docToChat(d)));
        return list;
    }

    public void update(Chat c) {
        col.updateOne(eq("_id", c.getId()),
                new Document("$set", new Document()
                        .append("activo", c.getActivo())
                        .append("fechaUltimoMensaje", c.getFechaUltimoMensaje())
                )
        );
    }
}