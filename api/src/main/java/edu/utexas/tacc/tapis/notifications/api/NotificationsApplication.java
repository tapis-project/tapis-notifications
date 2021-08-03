package edu.utexas.tacc.tapis.notifications.api;

import edu.utexas.tacc.tapis.notifications.api.factories.ServiceJWTCacheFactory;
import edu.utexas.tacc.tapis.notifications.api.providers.TopicsAuthz;
import edu.utexas.tacc.tapis.notifications.api.resources.Healthcheck;
import edu.utexas.tacc.tapis.notifications.api.resources.QueuesResource;
import edu.utexas.tacc.tapis.notifications.api.resources.TopicsResource;
import edu.utexas.tacc.tapis.notifications.lib.config.IRuntimeConfig;
import edu.utexas.tacc.tapis.notifications.lib.config.RuntimeParameters;
import edu.utexas.tacc.tapis.notifications.lib.config.RuntimeSettings;
import edu.utexas.tacc.tapis.notifications.lib.dao.NotificationsDAO;
import edu.utexas.tacc.tapis.notifications.lib.service.NotificationsPermissionsService;
import edu.utexas.tacc.tapis.notifications.lib.service.NotificationsService;
import edu.utexas.tacc.tapis.notifications.lib.service.QueueService;
import edu.utexas.tacc.tapis.notifications.lib.service.ServiceClientsFactory;
import edu.utexas.tacc.tapis.notifications.lib.service.ServiceContextFactory;
import edu.utexas.tacc.tapis.sharedapi.jaxrs.filters.JWTValidateRequestFilter;
import edu.utexas.tacc.tapis.shared.security.ServiceJWT;
import edu.utexas.tacc.tapis.shared.security.ServiceClients;
import edu.utexas.tacc.tapis.shared.security.ServiceContext;
import edu.utexas.tacc.tapis.shared.security.TenantManager;
import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.sharedapi.providers.ObjectMapperContextResolver;
import edu.utexas.tacc.tapis.sharedapi.providers.TapisExceptionMapper;
import edu.utexas.tacc.tapis.sharedapi.providers.ValidationExceptionMapper;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;
import java.net.URI;

/*
 * Main startup class for the web application. Uses Jersey and Grizzly frameworks.
 *   Performs setup for HK2 dependency injection.
 *   Registers packages and features for Jersey.
 *   Gets runtime parameters from the environment.
 *   Initializes the service:
 *     Init service context.
 *     DB creation or migration
 *   Starts the Grizzly server.
 *
 * The path here is appended to the context root and is configured to work when invoked in a standalone
 * container (command line) and in an IDE (such as eclipse).
 * ApplicationPath set to "/" since each resource class includes "/v3/apps" in the
 *     path set at the class level. See AppResource.java, PermsResource.java, etc.
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

  // For all logging use println or similar so we do not have a dependency on a logging subsystem.
  public NotificationsApplication()
  {
    // Log our existence.
    // Output version information on startup
    System.out.println("**** Starting Notifications Service. Version: " + TapisUtils.getTapisFullVersion() + " ****");

    // Needed for properly returning timestamps
    // Also allows for setting a breakpoint when response is being constructed.
    register(ObjectMapperContextResolver.class);

    // Register classes needed for returning a standard Tapis response for non-Tapis exceptions.
    register(TapisExceptionMapper.class);
    register(ValidationExceptionMapper.class);
    // Register service class for calling init method during application startup
//TODO    register(NotificationsServiceImpl.class);

    // We specify what packages JAX-RS should recursively scan to find annotations. By setting the value to the
    // top-level directory in all projects, we can use JAX-RS annotations in any tapis class. In particular, the filter
    // classes in tapis-shared-api will be discovered whenever that project is included as a maven dependency.
    packages("edu.utexas.tacc.tapis");

    // Set the application name. Note that this has no impact on base URL
    setApplicationName(TapisConstants.SERVICE_NAME_NOTIFICATIONS);

/*
        //our APIS
        register(TopicsResource.class);
        register(QueuesResource.class);
        register(Healthcheck.class);
        // Serialization
        register(JacksonFeature.class);
        // Custom Timestamp/Instant serialization
        register(ObjectMapperContextResolver.class);
        // ExceptionMappers, need both because ValidationMapper is a custom Jersey thing and
        // can't be implemented in a generic mapper
        register(TapisExceptionMapper.class);
        register(ValidationExceptionMapper.class);

        //Authorization Annotations
        register(TopicsAuthz.class);
        TenantManager tenantManager = TenantManager.getInstance(runtimeConfig.getTenantsServiceURL());
        tenantManager.getTenants();

        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(tenantManager).to(TenantManager.class);
                bindFactory(ServiceJWTCacheFactory.class).to(ServiceJWT.class).in(Singleton.class);
                bindAsContract(NotificationsService.class).in(Singleton.class);
                bindAsContract(NotificationsPermissionsService.class).in(Singleton.class);
                bindAsContract(QueueService.class).in(Singleton.class);
                bindAsContract(NotificationsDAO.class);
            }
        });
*/
    // Perform remaining init steps in try block so we can print a fatal error message if something goes wrong.
    try {
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
// TODO          bind(AppsServiceImpl.class).to(AppsService.class); // Used in Resource classes for most service calls
//          bind(AppsServiceImpl.class).to(AppsServiceImpl.class); // Used in AppsResource for checkDB
//          bind(AppsDaoImpl.class).to(AppsDao.class); // Used in service impl
          bindFactory(ServiceContextFactory.class).to(ServiceContext.class); // Used in service impl and AppsResource
          bindFactory(ServiceClientsFactory.class).to(ServiceClients.class); // Used in service impl
        }
      });
    } catch (Exception e) {
      // This is a fatal error
      System.out.println("**** FAILURE TO INITIALIZE: Tapis Applications Service ****");
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * Embedded Grizzly HTTP server
   */
  public static void main(String[] args) throws Exception
  {
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
//TODO    NotificationsServiceImpl svcImpl = locator.getService(NotificationsServiceImpl.class);

    // Call the main service init method
//TODO    svcImpl.initService(siteId, siteAdminTenantId, RuntimeParameters.getInstance().getServicePassword());
    // Create and start the server
    final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, config, false);
    server.start();
  }
}
