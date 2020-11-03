package edu.utexas.tacc.tapis.notifications.websockets;

import org.glassfish.hk2.api.ServiceLocator;

import javax.websocket.server.ServerEndpointConfig.Configurator;

/**
 * Instantiates WebSocket end-point with a custom injector so that @Inject can be
 * used normally.
 */
public class CustomConfigurator extends Configurator
{
    private ServiceLocator serviceLocator;

    public CustomConfigurator() {
        serviceLocator = Locator.getInstance();
    }

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException
    {
        return serviceLocator.getService(endpointClass);
    }
}
