package edu.utexas.tacc.tapis.notifications.websockets;


import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import org.jvnet.hk2.annotations.Service;

import javax.websocket.Session;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class SessionCache {

    private static final ConcurrentHashMap<String, Set<Session>> cache = new ConcurrentHashMap<>();
    private static final String KEY_FORMAT = "{0}.{1}";

    public void addSession(Session session) {
        AuthenticatedUser user = (AuthenticatedUser) session.getUserPrincipal();
        String key = MessageFormat.format(KEY_FORMAT, user.getTenantId(), user.getName());
        Set<Session> userSessions = cache.get(key);

        if (userSessions == null) {
            Set<Session> sessions = new HashSet<>();
            sessions.add(session);
            cache.put(key, sessions);
        } else {
            userSessions.add(session);
            cache.put(key, userSessions);
        }
    }

    public Set<Session> getUserSessions(AuthenticatedUser user) {
        String key = MessageFormat.format(KEY_FORMAT, user.getTenantId(), user.getName());
        return cache.get(key);
    }

}
