package edu.utexas.tacc.tapis.notifications.websockets;
import javax.inject.Singleton;
import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

import edu.utexas.tacc.tapis.notifications.lib.NotificationsService;
import edu.utexas.tacc.tapis.sharedapi.security.ITenantManager;
import edu.utexas.tacc.tapis.sharedapi.security.TenantManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Instantiates WebSocket end-point with a custom injector so that @Inject can be
 * used normally.
 */
public class CustomConfigurator extends Configurator
{
    private ServiceLocator serviceLocator;

    public CustomConfigurator() {
//        serviceLocator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
//        ServiceLocatorUtilities.enableImmediateScope(serviceLocator);
//        ServiceLocatorUtilities.bind(serviceLocator, new AbstractBinder() {
//
//            @Override
//            protected void configure() {
//                bind(AuthFilter.class).to(AuthFilter.class);
//                bind(Application.class).to(Application.class);
//                bind(UserEndpoint.class).to(UserEndpoint.class);
//                bind(NotificationsService.class).to(NotificationsService.class);
//                bind(TenantManager.class).to(ITenantManager.class);
//            }
//        });
    }

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException
    {
        return serviceLocator.getService(endpointClass);
    }
}
