package edu.utexas.tacc.tapis.notifications.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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

  private static final int SHUTDOWN_TIMEOUT_MS = 4000;

  // Number of buckets for grouping events for processing
  private static final int NUM_BUCKETS = 20;
  // Number of workers per bucket to start for handling notification delivery
  private static final int NUM_DELIVERY_WORKERS = 5;

  // Allow interrupt when shutting down executor services.
  private static final boolean mayInterruptIfRunning = true;

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

  // In-memory queues used to pass messages from rabbitmq to worker threads
  private final List<BlockingQueue<Delivery>> deliveryQueues = new ArrayList<>();

  // DeliveryBucketManagers for processing events, one per bucket
  private final List<Callable<String>> bucketManagers = new ArrayList<>();

  // ExecutorService and futures for bucket managers
  private final ExecutorService bucketManagerExecService =  Executors.newFixedThreadPool(NUM_BUCKETS);
  private List<Future<String>> bucketManagerFutures;

  // ExecutorService and future for subscription reaper
  private final ExecutorService reaperExecService = Executors.newSingleThreadExecutor();
  private Future<String> reaperFuture;

  // We must be running on a specific site and this will never change
  // These are initialized in method initService()
  private static String siteId;
  private static String siteAdminTenantId;
  public static String getSiteId() {return siteId;}
  public static String getServiceTenantId() {return siteAdminTenantId;}


  // ************************************************************************
  // *********************** Public Methods *********************************
  // ************************************************************************

  /*
   * Initialize the service:
   *   init service context
   *   migrate DB
   *   init message broker
   *   init in-memory queues for event processing
   *   init bucket manager callables
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

    // Create in-memory queues and callables for multi-threaded processing of events
    for (int i = 0; i < NUM_BUCKETS; i++)
    {
      deliveryQueues.add(new LinkedBlockingQueue<>());
      bucketManagers.add(new DeliveryBucketManager(deliveryQueues, i));
    }
  }

  /*
   * Final shut down of service
   */
  public void shutDown()
  {
    log.info(LibUtils.getMsg("NTFLIB_MSGBRKR_CONN_CLOSE", SHUTDOWN_TIMEOUT_MS));
    MessageBroker.getInstance().shutDown(SHUTDOWN_TIMEOUT_MS);
    // Force shutdown of executor services
    shutdownExecutors(SHUTDOWN_TIMEOUT_MS);
  }

  /*
   * Start the message broker consumer and
   *   start bucket managers that do the main work of sending out notifications based on subscriptions
   */
  public void processEvents() throws IOException, InterruptedException
  {
    // Start our basic consumer for main queue that handles incoming events
    // Consumer will compute bucket number for the event and hand it off to a bucket manager.
    String consumerTag = MessageBroker.getInstance().startConsumer(deliveryQueues);

    // Start up the bucket managers and wait for them to finish
    // The bucket managers should only finish on interrupt or error.
    bucketManagerFutures = bucketManagerExecService.invokeAll(bucketManagers);
  }

  /*
   * Start the reaper thread for cleaning up expired subscriptions
   */
  public void startReaper()
  {
    reaperFuture = reaperExecService.submit(new SubscriptionReaper());
  }

  /*
   * Stop the subscription reaper thread
   */
  public void stopReaper()
  {
    log.error("Stopping Subscription Reaper");
    reaperFuture.cancel(mayInterruptIfRunning);
  }

  /*
   * Stop the bucket managers
   */
  public void stopBucketManagers()
  {
    for (int i = 0; i < NUM_BUCKETS; i++)
    {
      log.info("Stopping Bucket Manager for bucket: " + i);
      bucketManagerFutures.get(i).cancel(mayInterruptIfRunning);
    }
  }


  // ************************************************************************
  // **************************  Private Methods  ***************************
  // ************************************************************************

  /*
   * Wait for all callables to finish
   */
  private void shutdownExecutors(int shutdownTimeout)
  {
    // Make sure reaper is shut down.
    log.info("Waiting for Subscription Reaper shutdown. Timeout in ms: " + shutdownTimeout);
    reaperExecService.shutdown();
    try
    {
      if (!reaperExecService.awaitTermination(shutdownTimeout, TimeUnit.MILLISECONDS)) reaperExecService.shutdown();
    }
    catch (InterruptedException e)
    {
      reaperExecService.shutdownNow();
    }

    // Make sure bucket managers are shut down.
    log.info("Waiting for shutdown of bucket managers. Timeout in ms: " + shutdownTimeout);
    bucketManagerExecService.shutdown();
    try
    {
      if (!bucketManagerExecService.awaitTermination(shutdownTimeout, TimeUnit.MILLISECONDS)) bucketManagerExecService.shutdown();
    }
    catch (InterruptedException e)
    {
      bucketManagerExecService.shutdownNow();
    }
  }
}
