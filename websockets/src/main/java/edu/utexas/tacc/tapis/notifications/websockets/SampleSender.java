package edu.utexas.tacc.tapis.notifications.websockets;

import edu.utexas.tacc.tapis.shared.notifications.Notification;
import edu.utexas.tacc.tapis.shared.notifications.TapisNotificationsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class SampleSender {

    private static final Logger log = LoggerFactory.getLogger(SampleSender.class);

    private static final TapisNotificationsClient client = new TapisNotificationsClient();

    public static void main(String[] args) throws Exception {


        // Sender, firing off messages every sec
        Flux.interval(Duration.ofMillis(100))
            .take(100)
            .flatMap(tick -> {
                Map<String, Object> body = new HashMap<>();
                body.put("test", "value");
                Notification note = new Notification.Builder()
                    .setTenant("dev")
                    .setRecipient("testuser2")
                    .setCreator("files")
                    .setBody(body)
                    .setLevel("INFO")
                    .setEventType("FILE_TRANSFER_PROGRESS")
                    .build();

                return client.sendNotificationAsync("dev.files.transfers.12345", note);
            })
            .subscribeOn(Schedulers.newBoundedElastic(8, 1, "sender"))
            .subscribe();

    }


}

