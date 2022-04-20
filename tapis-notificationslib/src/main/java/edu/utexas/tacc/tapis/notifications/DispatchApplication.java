package edu.utexas.tacc.tapis.notifications;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import edu.utexas.tacc.tapis.notifications.config.RuntimeParameters;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDao;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDaoImpl;
import edu.utexas.tacc.tapis.notifications.service.DispatchService;
import edu.utexas.tacc.tapis.shared.security.TenantManager;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;

/*
 * Main startup class for the Dispatch application.
 * The dispatcher application handles:
 *  - receiving events from the message broker
 *  - matching events with subscriptions which creates 0 or more notifications
 *  - sending notifications to subscribers
 *  - removing subscriptions when they expire
 * Note that an event may match 0 or more subscriptions and for each subscription matched there will be 1 or more
 *   notifications created. Each deliveryMethod in a subscription will result in a notification.
 * In-flight notifications are persisted.
 * Best effort is made to provide at least once delivery as well as recovery on errors and crashes.
 */
public class DispatchApplication
{
  // We must be running on a specific site and this will never change
  private static String siteId;
  public static String getSiteId() {return siteId;}
  private static String siteAdminTenantId;
  public static String getSiteAdminTenantId() {return siteAdminTenantId;}

  public static void main(String[] args) throws Exception
  {
    // Log our existence.
    // Output version information on startup
    System.out.printf("**** Starting Notifications Dispatch Service. Version: %s ****%n", TapisUtils.getTapisFullVersion());
    // Log our config
    System.out.println(RuntimeParameters.getInstance().getRuntimeParameters());

    // Get runtime parameters
    RuntimeParameters runParms = RuntimeParameters.getInstance();

    // Set site on which we are running. This is a required runtime parameter.
    siteId = runParms.getSiteId();

    // Initialize tenant manager singleton. This can be used by all subsequent application code.
    // The base url of the tenants service is a required input parameter.
    // Retrieve the tenant list from the tenant service now to fail fast if we cannot access the list.
    String url = runParms.getTenantsSvcURL();
    TenantManager.getInstance(url).getTenants();
    // Set admin tenant also, needed when building a client for calling other services (such as SK) as ourselves.
    siteAdminTenantId = TenantManager.getInstance(url).getSiteAdminTenantId(siteId);

    // Initialize bindings for HK2 dependency injection
    ServiceLocator locator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
    ServiceLocatorUtilities.bind(locator, new AbstractBinder() {
      @Override
      protected void configure()
      {
        bind(DispatchService.class).to(DispatchService.class); // Used here in this class.
        bind(NotificationsDaoImpl.class).to(NotificationsDao.class); // Used in DispatchService, DeliveryBucketManager
      }
    });

    // Get the service so we can do stuff.
    DispatchService dispatchService = locator.getService(DispatchService.class);

    // Call the main service init method. Setup DB, message broker, executor services, etc
    System.out.println("Initializing service");
    dispatchService.initService(siteAdminTenantId, RuntimeParameters.getInstance());

    // Add a shutdown hook so we can gracefully stop
    System.out.println("Registering shutdownHook");
    Thread shudownHook = new DispatchShutdown(dispatchService);
    Runtime.getRuntime().addShutdownHook(shudownHook);

    // Start background process to clean up expired subscriptions.
    System.out.println("Starting subscription reaper");
    dispatchService.startReaper(runParms.getNtfSubscriptionReaperInterval());

    // Start message broker consumer and bucket managers.
    // This is the main loop to process events while the service is running.
    System.out.println("Starting main loop for processEvents");
    dispatchService.processEvents();
    System.out.println("Finished main loop for processEvents");
  }

  /*
   *
   * Private class used to gracefully shut down the application
   *
   */
  private static class DispatchShutdown extends Thread
  {
    private final DispatchService svc;

    DispatchShutdown(DispatchService svc1) { svc = svc1; }

    @Override
    public void run()
    {
      System.out.printf("**** Stopping Notifications Dispatch Service. Version: %s ****%n", TapisUtils.getTapisFullVersion());
      // We are shutting down, stop the reaper and the bucket managers
      svc.stopReaper();
      // Perform any remaining shutdown steps
      svc.shutDown();
    }
  }
}
