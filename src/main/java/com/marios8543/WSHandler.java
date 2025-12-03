package com.marios8543;

import com.marios8543.musicsource.Song;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.*;

@WebSocket
public class WSHandler {
    private final Map<Session,String> users = new HashMap<>();
    public int nowListening = 0;
    public MessageBroadcast broadcast;

    public void broadcast(String author, String content) {
        JSONObject object = new JSONObject();
        object.put("author",author);
        object.put("content",content);
        object.put("nowListening", nowListening);

        users.keySet().forEach((session -> {
            try {
                session.getRemote().sendString(object.toJSONString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }

    public void requested(String ip , Song song) {
        Optional<Session> session = users.keySet().stream().filter(session1 -> session1.getRemoteAddress().getHostString().equals(ip)).findFirst();
        session.ifPresent(session1 -> {
            String username = users.get(session1);
            broadcast(username,String.format("Requested %s by %s",song.title,song.artist));
        });
    }

    public int getListening() {
        return users.size();
    }

    @OnWebSocketConnect
    public void connected(Session session) {
        String username = (new Username(session)).username;
        users.put(session,username);
        nowListening++;
        broadcast("",username+" has joined the chat!");
    }

    @OnWebSocketClose
    public void closed(Session session, int status, String reason) {
        System.out.println(reason);
        System.out.println(status);
        String username = users.get(session);
        users.remove(session);
        nowListening--;
        broadcast("",username+" has left the chat!");
    }

    @OnWebSocketMessage
    public void message(Session session, String message) {
        broadcast(users.get(session),message);
        broadcast.onMessage(users.get(session),message);
    }
}
