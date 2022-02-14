package edu.utexas.tacc.tapis.notifications.service;

import com.rabbitmq.client.DeliverCallback;
import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.notifications.config.RuntimeParameters;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDao;
import edu.utexas.tacc.tapis.notifications.model.Event;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
import edu.utexas.tacc.tapis.notifications.queue.MessageBrokerManager;
import edu.utexas.tacc.tapis.notifications.utils.LibUtils;
import edu.utexas.tacc.tapis.security.client.SKClient;
import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.security.ServiceClients;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static edu.utexas.tacc.tapis.shared.TapisConstants.NOTIFICATIONS_SERVICE;

/*
 * Notifications Dispatch Service.
 *   The dispatch service handles processing of notification events
 *   Uses Dao layer and other service library classes to perform all top level service operations.
 * Annotate as an hk2 Service so that default scope for Dependency Injection is singleton
 */
@Service
public class DispatchService
{
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************

  // Tracing.
  private static final Logger log = LoggerFactory.getLogger(DispatchService.class);

  // ************************************************************************
  // *********************** Enums ******************************************
  // ************************************************************************

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************

  // Use HK2 to inject singletons
//  @Inject
//  private NotificationsDao dao;

//  @Inject
//  private NotificationsService notifSvc;

  @Inject
  private ServiceClients serviceClients;

  // We must be running on a specific site and this will never change
  // These are initialized in method initService()
  private static String siteId;
  private static String siteAdminTenantId;
  public static String getSiteId() {return siteId;}
  public static String getServiceTenantId() {return siteAdminTenantId;}

  // ************************************************************************
  // *********************** Public Methods *********************************
  // ************************************************************************

  /**
   * Initialize the service:
   *   init service context
   *   migrate DB
   *   init message broker
   */
  public void initService(String siteAdminTenantId1, RuntimeParameters runParms) throws TapisException
  {
    // Initialize service context and site info
    siteId = runParms.getSiteId();
    siteAdminTenantId = siteAdminTenantId1;
    // Make sure DB is present and updated to latest version using flyway
//    dao.migrateDB();
    // TODO/TBD Initialize the singleton instance of the message broker manager
    MessageBrokerManager.init(runParms);
  }

  // -----------------------------------------------------------------------
  // ------------------------- Events -------------------------------
  // -----------------------------------------------------------------------

  /**
   * Start main loop for processing of events
   */
  public void processEvents() throws TapisException
  {
    log.error("TODO: Implement processEvents");
    try {Thread.sleep(5000);} catch (Exception e) {}
  }


  // ************************************************************************
  // **************************  Private Methods  ***************************
  // ************************************************************************
}
