package edu.utexas.tacc.tapis.notifications.websockets;

import edu.utexas.tacc.tapis.shared.notifications.TapisNotificationsClient;
import edu.utexas.tacc.tapis.sharedapi.security.TenantManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

public class Locator {

    private static ServiceLocator serviceLocator;

    public static ServiceLocator getInstance() {

        if (serviceLocator == null) {
            serviceLocator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
            AbstractBinder binder = new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(TenantManager.getInstance("https://dev.develop.tapis.io/")).to(TenantManager.class);
                    bindAsContract(AuthFilter.class);
                    bindAsContract(WebsocketApplication.class);
                    bindAsContract(UserEndpoint.class);
                    bindAsContract(TapisNotificationsClient.class);
                    bindAsContract(MessageDispatcher.class);
                }
            };

            ServiceLocatorUtilities.enableImmediateScope(serviceLocator);
            ServiceLocatorUtilities.bind(serviceLocator, binder);
            return serviceLocator;
        }
        return serviceLocator;
    }
}
