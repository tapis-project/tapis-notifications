package edu.utexas.tacc.tapis.notifications.service;

import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.notifications.config.RuntimeParameters;
import edu.utexas.tacc.tapis.notifications.utils.LibUtils;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.security.ServiceContext;

import static edu.utexas.tacc.tapis.shared.TapisConstants.SERVICE_NAME_NOTIFICATIONS;

import org.glassfish.hk2.api.Factory;

/**
 * HK2 Factory class providing a ServiceContext for the Notifications service.
 * ServiceContext is a singleton used to manage JWTs.
 * Binding happens in NotificationsApplication.java
 */
public class ServiceContextFactory implements Factory<ServiceContext>
{
  @Override
  public ServiceContext provide()
  {
    ServiceContext svcContext = ServiceContext.getInstance();
    RuntimeParameters runParms = RuntimeParameters.getInstance();
    try {
      svcContext.initServiceJWT(runParms.getSiteId(), SERVICE_NAME_NOTIFICATIONS, runParms.getServicePassword());
      return svcContext;
    }
    catch (TapisException | TapisClientException te)
    {
      String msg = LibUtils.getMsg("NTFLIB_SVCJWT_ERROR", runParms.getSiteId(), SERVICE_NAME_NOTIFICATIONS);
      throw new RuntimeException(msg, te);
    }
  }
  @Override
  public void dispose(ServiceContext c) {}
}
