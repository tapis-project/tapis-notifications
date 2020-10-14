package edu.utexas.tacc.tapis.notifications.lib;

import edu.utexas.tacc.tapis.notifications.lib.pojo.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

public class SampleSender {

    private static final Logger log = LoggerFactory.getLogger(SampleSender.class);

    private static final NotificationsService notificationsService = new NotificationsService();

    public static void main(String[] args) throws Exception{
        Flux.interval(Duration.ofMillis(1000))
            .publishOn(Schedulers.newElastic("sender"))
            .take(100)
            .flatMap(tick->{
                try {
                    Notification note = new Notification(
                        "dev",
                        "jmeiring",
                        "files",
                        "Hello world!",
                        "INFO");
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
