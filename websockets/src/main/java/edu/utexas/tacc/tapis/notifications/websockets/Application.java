package edu.utexas.tacc.tapis.notifications.websockets;

import edu.utexas.tacc.tapis.notifications.lib.NotificationsService;
import edu.utexas.tacc.tapis.sharedapi.security.TenantManager;
import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.*;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import static io.undertow.servlet.Servlets.defaultContainer;

public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private static final int port = 8080;

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


    public static void main(String[] args) throws Exception {

        ServiceLocator serviceLocator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
        ServiceLocatorUtilities.enableImmediateScope(serviceLocator);
        ServiceLocatorUtilities.bind(serviceLocator, new AbstractBinder() {

            @Override
            protected void configure() {
                bind(TenantManager.getInstance("https://master.tapis.io/v3/tenants/")).to(TenantManager.class);
                bindAsContract(AuthFilter.class);
                bind(Application.class).to(Application.class);
                bind(UserEndpoint.class).to(UserEndpoint.class);
                bind(NotificationsService.class).to(NotificationsService.class);
            }
        });

        final Xnio xnio = Xnio.getInstance("nio", Undertow.class.getClassLoader());
        final XnioWorker xnioWorker = xnio.createWorker(OptionMap.builder().getMap());

        UserEndpoint userEndpoint = serviceLocator.getService(UserEndpoint.class);
        AuthFilter authFilter = serviceLocator.getService(AuthFilter.class);
        FilterInstanceFactory filterFactory = new FilterInstanceFactory(authFilter);

        WebSocketDeploymentInfo webSocketDeploymentInfo = new WebSocketDeploymentInfo()
            .setWorker(xnioWorker)
            .addEndpoint(userEndpoint.getClass());


        DeploymentInfo deploymentInfo = Servlets.deployment()
            .setClassLoader(Application.class.getClassLoader())
            .setContextPath("/")
            .setDeploymentName("notifications")
            .addFilter(new FilterInfo("accessTokenFilter", authFilter.getClass(), filterFactory))
            .addFilterUrlMapping("accessTokenFilter", "/v3/notifications/*", DispatcherType.REQUEST)
            .addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, webSocketDeploymentInfo);

        DeploymentManager manager = defaultContainer().addDeployment(deploymentInfo);
        manager.deploy();

        log.info("Starting application deployment");
        Undertow server = Undertow.builder()
            .addHttpListener(8080, "localhost")
            .setHandler(manager.start())
            .build();
        server.start();
    }

}
