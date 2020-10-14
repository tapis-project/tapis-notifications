package edu.utexas.tacc.tapis.notifications.api;


import com.fasterxml.jackson.databind.ObjectMapper;
import edu.utexas.tacc.tapis.notifications.lib.TapisObjectMapper;
import edu.utexas.tacc.tapis.notifications.lib.NotificationsService;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.HashSet;
import java.util.Set;

@Service
@ServerEndpoint(value = "/notifications", configurator = AppConfig.class)
public class NotificationsResource {

    private static final Logger log = LoggerFactory.getLogger(NotificationsResource.class);
    private final NotificationsService notificationsService;
    private static Set<Session> sessions = new HashSet<>();
    final Scheduler scheduler = Schedulers.newElastic("messages");
    private static final ObjectMapper mapper = TapisObjectMapper.getMapper();

    // {tenant}.files.transfers.{UUID}
    // {tenant}.jobs.executions.{UUID}

    private String exchangeFormat = "{serviceName}.{actionName}.{UUID}";


    @Inject
    public NotificationsResource(NotificationsService notificationsService) {
        this.notificationsService = notificationsService;
        notificationsService.streamNotifications("#")
            .subscribeOn(scheduler)
            .log()
            .subscribe(m->{
                for (Session session: sessions) {
                    try {
                        session.getBasicRemote().sendText(mapper.writeValueAsString(m));
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }

            });
    }

    @OnOpen
    public void onOpen(Session s) {
        sessions.add(s);
    }

    @OnClose
    public void onClose(Session s) {
        sessions.remove(s);
    }

}
