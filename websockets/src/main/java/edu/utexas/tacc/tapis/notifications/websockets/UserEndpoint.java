package edu.utexas.tacc.tapis.notifications.websockets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.utexas.tacc.tapis.shared.notifications.TapisNotificationsClient;
import edu.utexas.tacc.tapis.shared.utils.TapisObjectMapper;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.OnMessage;
import javax.websocket.OnError;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Set;

@ServerEndpoint(
    value = "/v3/notifications",
    configurator = CustomConfigurator.class
)
public class UserEndpoint {

    private static final ObjectMapper mapper = TapisObjectMapper.getMapper();

    @Inject
    UserNotificationService userNotificationService;

    @Inject
    SessionCache sessionCache;

    private Disposable subscription;


    @OnOpen
    public void onOpen(Session session) throws IOException {
        AuthenticatedUser user = (AuthenticatedUser) session.getUserPrincipal();
        session.getBasicRemote().sendText("Hello!");
        sessionCache.addSession(session);
        subscription = userNotificationService.streamUserNotifications(user)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe((UserNotification notification)->{
                try {
                    String message = mapper.writeValueAsString(notification);
                    Set<Session> userSessions = sessionCache.getUserSessions(user);
                    for(Session s : userSessions) {
                        s.getBasicRemote().sendText(message);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
    }

    @OnMessage
    public String echo(String message) {
        return message + " (from your server)";
    }

    @OnError
    public void onError(Throwable t) {
        t.printStackTrace();
    }

    @OnClose
    public void onClose(Session session) {
        if (subscription != null) subscription.dispose();
    }
}
