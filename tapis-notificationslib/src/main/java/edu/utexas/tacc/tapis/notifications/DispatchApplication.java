package edu.utexas.tacc.tapis.notifications;

import edu.utexas.tacc.tapis.notifications.config.RuntimeParameters;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDao;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDaoImpl;
import edu.utexas.tacc.tapis.notifications.service.DispatchService;
import edu.utexas.tacc.tapis.notifications.service.NotificationsService;
import edu.utexas.tacc.tapis.notifications.service.NotificationsServiceImpl;
import edu.utexas.tacc.tapis.notifications.service.ServiceClientsFactory;
import edu.utexas.tacc.tapis.notifications.service.ServiceContextFactory;
import edu.utexas.tacc.tapis.shared.security.ServiceClients;
import edu.utexas.tacc.tapis.shared.security.ServiceContext;
import edu.utexas.tacc.tapis.shared.security.TenantManager;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import javax.inject.Singleton;

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
        bind(NotificationsDao.class).to(NotificationsDaoImpl.class); // Used in DispatchService, DeliveryWorker
//        bind(NotificationsService.class).to(NotificationsServiceImpl.class); // Used in Dispatch service
//        bindFactory(ServiceClientsFactory.class).to(ServiceClients.class); // TODO/TBD: Used in Dispatch service
      }
    });

    DispatchService dispatchService = locator.getService(DispatchService.class);

    // Call the main service init method. Setup DB and message broker.
    dispatchService.initService(siteAdminTenantId, RuntimeParameters.getInstance());

    // Start background thread to clean up expired subscriptions.
    dispatchService.startReaper();

    // Start the worker threads that send out notifications
    dispatchService.startWorkers();

    // Start the main loop to process events and hand them out to workers.
    dispatchService.processEvents();

    System.out.println("**** Stopping Notifications Dispatch Service. Version: " + TapisUtils.getTapisFullVersion() + " ****");

    // Stop the workers that send out notifications
    dispatchService.stopWorkers();

    // We are shutting down, stop the reaper thread.
    dispatchService.stopReaper();

    dispatchService.shutDown();
    System.exit(0);
  }
}
