package edu.utexas.tacc.tapis.notifications.websockets;

import edu.utexas.tacc.tapis.shared.notifications.TapisNotificationsClient;
import edu.utexas.tacc.tapis.sharedapi.security.TenantManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import reactor.core.scheduler.Schedulers;

public class DispatcherApplication {


    public static void main(String[] args) {
        ServiceLocator serviceLocator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
        ServiceLocatorUtilities.enableImmediateScope(serviceLocator);
        ServiceLocatorUtilities.bind(serviceLocator, new AbstractBinder() {

            @Override
            protected void configure() {
                bind(TenantManager.getInstance("https://master.tapis.io/v3/tenants/")).to(TenantManager.class);
                bindAsContract(MessageDispatcher.class);
                bindAsContract(UserNotificationService.class);
                bind(TapisNotificationsClient.class).to(TapisNotificationsClient.class);
            }
        });

        MessageDispatcher dispatcher = serviceLocator.getService(MessageDispatcher.class);

        dispatcher.dispatchMessages()
            .subscribeOn(Schedulers.boundedElastic())
            .log()
            .subscribe();
    }
}
