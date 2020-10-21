package edu.utexas.tacc.tapis.notifications.frontend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TextWebSocketHandler.class);

    ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {

//        sessions.forEach( (key, ses)-> {
//            Map value = new Gson().fromJson(message.getPayload(), Map.class);
//            try {
//                ses.sendMessage(new TextMessage("Hello " + value.get("name") + " !"));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Principal user = session.getPrincipal();
//        sessions.add(session);
    }
}