package edu.utexas.tacc.tapis.notifications.lib;

import edu.utexas.tacc.tapis.notifications.lib.cache.TopicsCache;
import edu.utexas.tacc.tapis.notifications.lib.dao.NotificationsDAO;
import edu.utexas.tacc.tapis.notifications.lib.services.NotificationDispatcherService;
import edu.utexas.tacc.tapis.notifications.lib.services.NotificationsService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import javax.inject.Singleton;

public class Application {



    public static void main(String[] args) throws Exception {

        ServiceLocator locator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
        ServiceLocatorUtilities.bind(new AbstractBinder() {
            @Override
            protected void configure() {
                bindAsContract(TopicsCache.class).in(Singleton.class);
                bindAsContract(NotificationsDAO.class);
                bindAsContract(NotificationsService.class).in(Singleton.class);
                bindAsContract(NotificationDispatcherService.class).in(Singleton.class);
            }
        });

        NotificationDispatcherService dispatcherService = locator.getService(NotificationDispatcherService.class);

        dispatcherService
            .processMessages()
            .subscribe();

    }

}
