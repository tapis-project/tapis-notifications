package edu.utexas.tacc.tapis.notifications.websockets;

import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.glassfish.hk2.api.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import static io.undertow.servlet.Servlets.defaultContainer;

public class WebsocketApplication {
    private static final Logger log = LoggerFactory.getLogger(WebsocketApplication.class);

    private static final int port = 8080;


    // Internal class just to manage the injected dependencies in the AuthFilter
    private static class FilterInstanceFactory implements InstanceFactory<Filter> {

        private final Filter filter;

        private FilterInstanceFactory(Filter filter) {
            this.filter = filter;
        }

        @Override
        public InstanceHandle<Filter> createInstance() throws InstantiationException {
            return new InstanceHandle<>() {
                @Override
                public Filter getInstance() {
                    return filter;
                }
                @Override
                public void release() {}
            };
        }
    }


    public static void main(String[] args) throws Exception{
        ServiceLocator serviceLocator = Locator.getInstance();
        buildAndStartServer(serviceLocator);
    }

    public static void buildAndStartServer(ServiceLocator serviceLocator) throws Exception {


        final Xnio xnio = Xnio.getInstance("nio", Undertow.class.getClassLoader());
        final XnioWorker xnioWorker = xnio.createWorker(OptionMap.builder().getMap());

        UserEndpoint userEndpoint = serviceLocator.getService(UserEndpoint.class);
        AuthFilter authFilter = serviceLocator.getService(AuthFilter.class);
        FilterInstanceFactory filterFactory = new FilterInstanceFactory(authFilter);

        WebSocketDeploymentInfo webSocketDeploymentInfo = new WebSocketDeploymentInfo()
            .setWorker(xnioWorker)
            .addEndpoint(userEndpoint.getClass());


        DeploymentInfo deploymentInfo = Servlets.deployment()
            .setClassLoader(userEndpoint.getClass().getClassLoader())
            .setContextPath("/")
            .setDeploymentName("notifications")
            .addFilter(new FilterInfo("accessTokenFilter", authFilter.getClass(), filterFactory))
            .addFilterUrlMapping("accessTokenFilter", "/v3/notifications/*", DispatcherType.REQUEST)
            .addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, webSocketDeploymentInfo);

        DeploymentManager manager = defaultContainer().addDeployment(deploymentInfo);
        manager.deploy();

        log.info("Starting application deployment");
        Undertow server = Undertow.builder()
            .addHttpListener(port, "localhost")
            .setHandler(manager.start())
            .build();
        server.start();
    }

}
