package edu.utexas.tacc.tapis.notifications.lib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.scheduler.Schedulers;

public class SampleReceiver {

    private static final Logger log = LoggerFactory.getLogger(SampleReceiver.class);

    private static final NotificationsService notificationsService = new NotificationsService();

    public static void main(String[] args) throws Exception{

        notificationsService.streamNotifications("#")
            .subscribeOn(Schedulers.newBoundedElastic(8, 100, "receiver"))
            .log()
            .subscribe();
    }


}
