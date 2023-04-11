package edu.utexas.tacc.tapis.notifications.api;

import java.net.URI;
import javax.ws.rs.ApplicationPath;

import edu.utexas.tacc.tapis.notifications.api.resources.EventResource;
import edu.utexas.tacc.tapis.notifications.api.resources.GeneralResource;
import edu.utexas.tacc.tapis.notifications.api.resources.SubscriptionResource;
import edu.utexas.tacc.tapis.notifications.api.resources.TestSequenceResource;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;

import edu.utexas.tacc.tapis.notifications.config.RuntimeParameters;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDao;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDaoImpl;
import edu.utexas.tacc.tapis.notifications.service.NotificationsService;
import edu.utexas.tacc.tapis.notifications.service.NotificationsServiceImpl;
import edu.utexas.tacc.tapis.notifications.service.ServiceClientsFactory;
import edu.utexas.tacc.tapis.notifications.service.ServiceContextFactory;
import edu.utexas.tacc.tapis.shared.security.ServiceClients;
import edu.utexas.tacc.tapis.shared.security.ServiceContext;
import edu.utexas.tacc.tapis.shared.security.TenantManager;
import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.sharedapi.jaxrs.filters.JWTValidateRequestFilter;
import edu.utexas.tacc.tapis.sharedapi.providers.ApiExceptionMapper;
import edu.utexas.tacc.tapis.sharedapi.providers.ObjectMapperContextResolver;
import edu.utexas.tacc.tapis.sharedapi.providers.ValidationExceptionMapper;

/*
 * Main startup class for the web application. Uses Jersey and Grizzly frameworks.
 *   Performs setup for HK2 dependency injection.
 *   Register classes and features for Jersey.
 *   Gets runtime parameters from the environment.
 *   Initializes the service:
 *     Init service context.
 *     DB creation or migration
 *     Message broker setup
 *   Starts the Grizzly server.
 *
 * The path here is appended to the context root and is configured to work when invoked in a standalone
 * container (command line) and in an IDE (such as eclipse).
 * ApplicationPath set to "/" since each resource class includes "/v3/notifications" in the
 *     path set at the class level. See SubscriptionResource.java, etc.
 *     This has been found to be a more robust scheme for keeping startup working for both
 *     running in an IDE and standalone.
 */
@ApplicationPath("/")
public class NotificationsApplication extends ResourceConfig
{
  // We must be running on a specific site and this will never change
  private static String siteId;
  public static String getSiteId() {return siteId;}
  private static String siteAdminTenantId;
  public static String getSiteAdminTenantId() {return siteAdminTenantId;}

  // For all logging use println or similar, so we do not have a dependency on a logging subsystem.
  public NotificationsApplication()
  {
    // Needed for properly returning timestamps
    // Also allows for setting a breakpoint when response is being constructed.
    register(ObjectMapperContextResolver.class);

    // Register classes needed for returning a standard Tapis response for non-Tapis exceptions.
    register(ApiExceptionMapper.class);
    register(ValidationExceptionMapper.class);

    //JWT validation
    register(JWTValidateRequestFilter.class);

    //Our APIs
    register(GeneralResource.class);
    register(SubscriptionResource.class);
    register(EventResource.class);
    register(TestSequenceResource.class);

    // Set the application name. Note that this has no impact on base URL
    setApplicationName(TapisConstants.SERVICE_NAME_NOTIFICATIONS);

    // Perform remaining init steps in try block, so that we can print a fatal error message if something goes wrong.
    try
    {
      // Get runtime parameters
      RuntimeParameters runParms = RuntimeParameters.getInstance();

      // Set site on which we are running. This is a required runtime parameter.
      siteId = runParms.getSiteId();

      // Initialize security filter used when processing a request.
      JWTValidateRequestFilter.setService(TapisConstants.SERVICE_NAME_NOTIFICATIONS);
      JWTValidateRequestFilter.setSiteId(siteId);

      // Initialize tenant manager singleton. This can be used by all subsequent application code, including filters.
      // The base url of the tenants service is a required input parameter.
      // Retrieve the tenant list from the tenant service now to fail fast if we cannot access the list.
      String url = runParms.getTenantsSvcURL();
      TenantManager.getInstance(url).getTenants();
      // Set admin tenant also, needed when building a client for calling other services (such as SK) as ourselves.
      siteAdminTenantId = TenantManager.getInstance(url).getSiteAdminTenantId(siteId);

      // Initialize bindings for HK2 dependency injection
      register(new AbstractBinder() {
        @Override
        protected void configure() {
          bind(NotificationsServiceImpl.class).to(NotificationsService.class); // Used in Resource classes for most service calls
          bind(NotificationsServiceImpl.class).to(NotificationsServiceImpl.class); // Used in GeneralResource for checkDB
          bind(NotificationsDaoImpl.class).to(NotificationsDao.class); // Used in service impl
          bindFactory(ServiceContextFactory.class).to(ServiceContext.class); // Used in service impl and GeneralResource
          bindFactory(ServiceClientsFactory.class).to(ServiceClients.class); // Used in service impl
        }
      });
    } catch (Exception e) {
      // This is a fatal error
      System.out.println("**** FAILURE TO INITIALIZE: Tapis Notifications Service ****");
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * Embedded Grizzly HTTP server
   */
  public static void main(String[] args) throws Exception
  {
    // Log our existence.
    // Output version information on startup
    System.out.printf("**** Starting Notifications Service. Version: %s ****%n", TapisUtils.getTapisFullVersion());
    // Log our config
    System.out.println(RuntimeParameters.getInstance().getRuntimeParameters());

    // If TAPIS_SERVICE_PORT set in env then use it.
    // Useful for starting service locally on a busy system where 8080 may not be available.
    String servicePort = System.getenv("TAPIS_SERVICE_PORT");
    if (StringUtils.isBlank(servicePort)) servicePort = "8080";

    // Set base protocol and port. If mainly running in k8s this may not need to be configurable.
    final URI baseUri = URI.create("http://0.0.0.0:" + servicePort + "/");

    // Initialize the application container
    NotificationsApplication config = new NotificationsApplication();

    // Initialize the service
    // In order to instantiate our service class using HK2 we need to create an application handler
    //   which allows us to get an injection manager which is used to get a locator.
    //   The locator is used get classes that have been registered using AbstractBinder.
    // NOTE: As of Jersey 2.26 dependency injection was abstracted out to make it easier to use DI frameworks
    //       other than HK2, although finding docs and examples on how to do so seems difficult.
    ApplicationHandler handler = new ApplicationHandler(config);
    InjectionManager im = handler.getInjectionManager();
    ServiceLocator locator = im.getInstance(ServiceLocator.class);
    NotificationsServiceImpl svcImpl = locator.getService(NotificationsServiceImpl.class);

    // Call the main service init method. Setup service context, DB and message broker.
    System.out.println("Initializing service");
    svcImpl.initService(siteAdminTenantId, RuntimeParameters.getInstance());

    System.out.println("Registering shutdownHook");
    // Add a shutdown hook so we can gracefully stop
    Thread shudownHook = new NotificationsApplication.ServiceShutdown(svcImpl);
    Runtime.getRuntime().addShutdownHook(shudownHook);

    System.out.println("Starting http server");
    // Create and start the server
    final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, config, false);
    server.start();
  }

  /*
   *
   * Private class used to gracefully shut down the application
   *
   */
  private static class ServiceShutdown extends Thread
  {
    private final NotificationsService svc;

    ServiceShutdown(NotificationsService svc1)
    {
      svc = svc1;
    }

    @Override
    public void run()
    {
      System.out.printf("**** Stopping Notifications Service. Version: %s ****%n", TapisUtils.getTapisFullVersion());
      // Perform any remaining shutdown steps
//      svc.shutDown();
    }
  }
}
