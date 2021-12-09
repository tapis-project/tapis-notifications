package edu.utexas.tacc.tapis.notifications;

import edu.utexas.tacc.tapis.notifications.cache.TopicsCache;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDAO;
import edu.utexas.tacc.tapis.notifications.service.NotificationDispatcherService;
import edu.utexas.tacc.tapis.notifications.service.NotificationsService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkerApplication
{

    private static final Logger log = LoggerFactory.getLogger(WorkerApplication.class);

    public static void main(String[] args) throws Exception {

        AtomicInteger counter = new AtomicInteger();

        ServiceLocator locator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
        ServiceLocatorUtilities.bind(locator, new AbstractBinder() {
            @Override
            protected void configure() {
                bindAsContract(TopicsCache.class).in(Singleton.class);
                bindAsContract(NotificationsDAO.class);
                bindAsContract(NotificationsService.class).in(Singleton.class);
                bindAsContract(NotificationDispatcherService.class).in(Singleton.class);
            }
        });

        NotificationDispatcherService dispatcherService = locator.getService(NotificationDispatcherService.class);

// TODO
// TODO       dispatcherService
//            .processMessages()
//            .subscribe((message)-> {
//                int currentCount = counter.incrementAndGet();
//                log.info("Current message count={}", currentCount);
//                log.info(message.toString());
//            });

    }

}
