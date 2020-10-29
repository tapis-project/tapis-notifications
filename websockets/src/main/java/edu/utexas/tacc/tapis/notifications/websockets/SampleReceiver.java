package edu.utexas.tacc.tapis.notifications.websockets;

import edu.utexas.tacc.tapis.notifications.lib.NotificationsService;
import edu.utexas.tacc.tapis.notifications.lib.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

public class SampleReceiver {

    private static final Logger log = LoggerFactory.getLogger(SampleReceiver.class);

    private static final NotificationsService notificationsService = new NotificationsService();

    public static void main(String[] args) throws Exception{

        notificationsService.streamNotifications("*.files.*")
            .subscribeOn(Schedulers.newBoundedElastic(8, 100, "receiver"))
            .log()
            .subscribe();
    }


    public static class SampleSender {

        private static final Logger log = LoggerFactory.getLogger(SampleSender.class);

        private static final NotificationsService notificationsService = new NotificationsService();

        public static void main(String[] args) throws Exception{
            Flux.interval(Duration.ofMillis(1000))
                .publishOn(Schedulers.newElastic("sender"))
                .take(100)
                .flatMap(tick->{
                    try {
                        Notification note = new Notification.Builder()
                            .setTenant("dev")
                            .setRecipient("jmeiring")
                            .setCreator("files")
                            .setBody("Hello world!")
                            .setLevel("INFO")
                            .setEventType("FILE_TRANSFER_PROGRESS")
                            .build();
                        notificationsService.sendNotification("dev.files.transfers.12345", note);
                    } catch (ServiceException ex) {
                        log.error(ex.getLocalizedMessage(), ex);
                        return Flux.empty();
                    }
                    return Flux.empty();
                }).subscribe(
                    m->{log.info(m.toString());}
                );
        }


    }
}
