package edu.utexas.tacc.tapis.notifications.websockets;

import edu.utexas.tacc.tapis.shared.notifications.Notification;
import edu.utexas.tacc.tapis.shared.notifications.TapisNotificationsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Duration;

public class SampleApp {

    private static final Logger log = LoggerFactory.getLogger(SampleApp.class);

    private static final TapisNotificationsClient client = new TapisNotificationsClient();

    public static void main(String[] args) throws Exception {


        // Sender, firing off messages every sec
        Flux.interval(Duration.ofMillis(1000))
            .publishOn(Schedulers.newElastic("sender"))
            .take(100)
            .flatMap(tick -> {
                try {
                    Notification note = new Notification.Builder()
                        .setTenant("dev")
                        .setRecipient("jmeiring")
                        .setCreator("files")
                        .setBody("Hello world!")
                        .setLevel("INFO")
                        .setEventType("FILE_TRANSFER_PROGRESS")
                        .build();
                    client.sendNotification("dev.files.transfers.12345", note);
                } catch (IOException ex) {
                    log.error(ex.getLocalizedMessage(), ex);
                    return Flux.empty();
                }
                return Flux.empty();
            })
            .subscribe( m -> log.info(m.toString())
        );


        // Listener
        client.streamNotifications("*.files.*")
            .subscribeOn(Schedulers.newBoundedElastic(8, 100, "receiver"))
            .subscribe(notification -> log.info(notification.toString()));

    }


}

