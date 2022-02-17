package edu.utexas.tacc.tapis.notifications.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;

import edu.utexas.tacc.tapis.notifications.config.RuntimeParameters;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDao;
import edu.utexas.tacc.tapis.notifications.utils.LibUtils;
import edu.utexas.tacc.tapis.notifications.model.Delivery;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;


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

  private static final int SHUTDOWN_TIMEOUT_MS = 10000;

  // TODO: read this from env variable
  private static final int DEFAULT_NUM_BUCKETS = 1;

  // ************************************************************************
  // *********************** Enums ******************************************
  // ************************************************************************

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************

  // Use HK2 to inject singletons
  @Inject
  private NotificationsDao dao;

//  @Inject
//  private NotificationsService notifSvc;

//  @Inject
//  private ServiceClients serviceClients;

  // In-memory queues used to pass messages from rabbitmq to worker threads
  private final List<BlockingQueue<Delivery>> deliveryQueues = new ArrayList<>();

  // DeliveryWorkers for processing events, one per bucket
  private final List<DeliveryWorker> deliveryWorkers = new ArrayList<>();

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
   *   init in-memory queues for event processing
   */
  public void initService(String siteAdminTenantId1, RuntimeParameters runParms) throws TapisException
  {
    // Initialize service context and site info
    siteId = runParms.getSiteId();
    siteAdminTenantId = siteAdminTenantId1;

    // Make sure DB is present and updated to the latest version using flyway
    dao.migrateDB();

    // Initialize the singleton instance of the message broker manager
    MessageBroker.init(runParms);

    // Create in-memory queues and workers for multi-threaded processing of events
    for (int i = 0; i < DEFAULT_NUM_BUCKETS; i++)
    {
      deliveryQueues.add(new LinkedBlockingQueue<>());
      deliveryWorkers.add(new DeliveryWorker(deliveryQueues, i));
    }
  }

  /**
   * Stop the service
   */
  public void shutDown()
  {
    log.info(LibUtils.getMsg("NTFLIB_MSGBRKR_CONN_CLOSE", SHUTDOWN_TIMEOUT_MS));
    MessageBroker.getInstance().shutDown(SHUTDOWN_TIMEOUT_MS);
  }

  // -----------------------------------------------------------------------
  // ------------------------- Events -------------------------------
  // -----------------------------------------------------------------------

  /**
   * Start main loop for processing of events
   */
  public void processEvents() throws IOException
  {
    // Start our basic consumer for main queue that handles incoming events
    String consumerTag = MessageBroker.getInstance().startConsumer(deliveryQueues);

    // TODO: Wait for event processing worker threads to complete
    waitForShutdown();

    log.error("TODO: Implement processEvents");
    try {Thread.sleep(5000);} catch (Exception e) {}
  }

  /**
   * Start the reaper thread for cleaning up expired subscriptions
   */
  public void startReaper() throws TapisException
  {
    log.error("TODO: Implement startReaper");
    try {Thread.sleep(2000);} catch (Exception e) {}
  }

  /**
   * Stop the reaper thread for cleaning up expired subscriptions
   */
  public void stopReaper() throws TapisException
  {
    log.error("TODO: Implement stopReaper");
    try {Thread.sleep(1000);} catch (Exception e) {}
  }

  /**
   * Start the workers that would do the main work of sending out notifications based on subscriptions
   */
  public void startWorkers() throws TapisException
  {
    for (DeliveryWorker worker : deliveryWorkers)
    {
      log.info("Starting worker for bucket: {}", worker.getBucketNum());
      worker.start();
      try {log.info("Sleep 1 second"); Thread.sleep(1000);} catch (Exception e) {}
    }
  }

  /**
   * Stop the workers
   */
  public void stopWorkers() throws TapisException
  {
    log.error("TODO: Implement stopWorkers");
    try {Thread.sleep(1000);} catch (Exception e) {}
  }


  // ************************************************************************
  // **************************  Private Methods  ***************************
  // ************************************************************************

  /*
   * Wait for worker threads to finish
   */
  private void waitForShutdown()
  {
    // TODO
    log.error("TODO: Implement waitForShutdown: sleeping for 300 seconds ...");
    try {Thread.sleep(300000);} catch (Exception e) {}

  }
}