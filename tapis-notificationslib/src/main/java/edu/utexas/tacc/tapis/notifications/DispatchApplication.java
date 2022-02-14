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
    System.out.println("**** Starting Notifications Dispatch Service. Version: " + TapisUtils.getTapisFullVersion() + " ****");

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

    ServiceLocator locator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
    ServiceLocatorUtilities.bind(locator, new AbstractBinder() {
      @Override
      protected void configure()
      {
        bind(DispatchService.class).to(DispatchService.class);
        bind(NotificationsDao.class).to(NotificationsDaoImpl.class); // Used in Dispatch service
        bind(NotificationsService.class).to(NotificationsServiceImpl.class); // Used in Dispatch service
        bindFactory(ServiceClientsFactory.class).to(ServiceClients.class); // TODO/TBD: Used in Dispatch service
      }
    });

    DispatchService dispatchService = locator.getService(DispatchService.class);

    // Call the main service init method. Setup DB and message broker.
    dispatchService.initService(siteAdminTenantId, RuntimeParameters.getInstance());

    // Start background thread to clean up expired subscriptions.
    // TODO dispatchService.startReaper();

    // Start the main loop that processes events.
    dispatchService.processEvents();

    // We are shutting down, stop the reaper thread.
    // TODO dispatchService.stopReaper();

    System.out.println("**** Stopping Notifications Dispatch Service. Version: " + TapisUtils.getTapisFullVersion() + " ****");
    System.exit(0);
  }
}
