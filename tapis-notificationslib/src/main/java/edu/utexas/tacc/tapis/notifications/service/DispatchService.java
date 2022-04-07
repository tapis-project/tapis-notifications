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
import java.util.concurrent.ScheduledExecutorService;
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

  // Logging
  private static final Logger log = LoggerFactory.getLogger(DispatchService.class);

  // When shutting down background processes give them 5 seconds to finish
  private static final int SHUTDOWN_TIMEOUT_MS = 5000;

  // Number of buckets for grouping events for processing
  // Use a prime number since this should improve hash performance
  // Leave hard-coded, changing would cause trouble for re-starts and recovery
  //  - would need to quiesce and clean out queue.
  //  - e.g., reducing number could lead to orphan entries
  //          increasing number could lead to events moving from one bucket to another which would break recovery
  public static final int NUM_BUCKETS = 23;

  // Default number of workers per bucket for handling notification delivery. Can be changed via runtime parameter.
  public static final int DEFAULT_NUM_DELIVERY_WORKERS = 5;
  // Default interval in minutes to use when periodically running the subscription reaper. Can be changed via runtime parameter.
  public static final int DEFAULT_SUBSCR_REAPER_INTERVAL = 3;

  // Number of attempts during initial delivery and interval (in seconds) between each one
  public static final int DEFAULT_DELIVERY_ATTEMPTS = 4;
  public static final int DEFAULT_DELIVERY_RETRY_INTERVAL = 20;

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

  // In-memory queues used to pass messages from rabbitmq to bucket managers
  private final List<BlockingQueue<Delivery>> deliveryBucketQueues = new ArrayList<>();

  // DeliveryBucketManagers for processing events, one per bucket
  private final List<Callable<String>> bucketManagers = new ArrayList<>();

  // ExecutorService for bucket managers
  private final ExecutorService bucketManagerExecService =  Executors.newFixedThreadPool(NUM_BUCKETS);

  // ExecutorService and future for subscription reaper
  private final ScheduledExecutorService reaperExecService = Executors.newSingleThreadScheduledExecutor();
  private Future<?> reaperTaskFuture;

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
    log.info(LibUtils.getMsg("NTFLIB_DSP_MIGRATE_DB"));
    dao.migrateDB();

    // Initialize the singleton instance of the message broker manager
    log.info(LibUtils.getMsg("NTFLIB_DSP_INIT_MSGBRKR"));
    MessageBroker.init(runParms);

    // Create in-memory queues and callables for multi-threaded processing of events
    for (int i = 0; i < NUM_BUCKETS; i++)
    {
      deliveryBucketQueues.add(new LinkedBlockingQueue<>());
      bucketManagers.add(new DeliveryBucketManager(dao, deliveryBucketQueues.get(i), i));
    }
  }

  /*
   * Start the message broker consumer and start bucket managers that do the main work of sending out
   *   notifications based on subscriptions.
   * This is expected to run until the dispatcher shuts down.
   */
  public void processEvents() throws IOException, InterruptedException
  {
    // Start our basic consumer for main queue.
    // Consumer handles incoming events.
    // Consumer will compute bucket number for the event and hand it off to a bucket manager.
    String consumerTag = MessageBroker.getInstance().startConsumer(deliveryBucketQueues);

    // Start up the bucket managers and wait for them to finish
    // The bucket managers will only finish on interrupt or error.
    bucketManagerExecService.invokeAll(bucketManagers);
  }

  /**
   * Start the reaper thread for cleaning up expired subscriptions
   * The reaper is a ScheduledExecutorService that runs periodically using the value passed in as the period in minutes.
   *
   * @param intervalMinutes execution period in minutes
   */
  public void startReaper(long intervalMinutes)
  {
    log.info(LibUtils.getMsg("NTFLIB_DSP_REAPER_START"));
    reaperTaskFuture = reaperExecService.scheduleAtFixedRate(() -> SubscriptionReaper.cleanup(dao),
                                                             intervalMinutes, intervalMinutes, TimeUnit.MINUTES);
  }

  /*
   * Stop the subscription reaper thread
   */
  public void stopReaper()
  {
    log.info(LibUtils.getMsg("NTFLIB_DSP_REAPER_STOP"));
    if (reaperTaskFuture != null) reaperTaskFuture.cancel(mayInterruptIfRunning);
  }

  /*
   * Final shut down of service
   */
  public void shutDown()
  {
    MessageBroker.getInstance().shutDown(SHUTDOWN_TIMEOUT_MS);
    // Force shutdown of executor services
    shutdownExecutors(SHUTDOWN_TIMEOUT_MS);
  }

  // ************************************************************************
  // **************************  Private Methods  ***************************
  // ************************************************************************

  /*
   * Shut down Executors after giving tasks some time to finish up.
   */
  private void shutdownExecutors(int shutdownTimeout)
  {
    // Make sure reaper is shut down.
    log.info(LibUtils.getMsg("NTFLIB_DSP_REAPER_SHUT", shutdownTimeout));
    // Stop the service from accepting any new tasks.
    reaperExecService.shutdown();
    try
    {
      // Give any running tasks some time to complete before forcing a shutdown
      if (!reaperExecService.awaitTermination(shutdownTimeout, TimeUnit.MILLISECONDS))
        reaperExecService.shutdownNow();
    }
    catch (InterruptedException e)
    {
      // We may have been interrupted waiting to finish. Force it to complete.
      reaperExecService.shutdownNow();
    }

    // Make sure bucket managers are shut down.
    log.info(LibUtils.getMsg("NTFLIB_DSP_SHUT_BUCKETS", shutdownTimeout));
    // Stop the service from accepting any new tasks.
    bucketManagerExecService.shutdown();
    try
    {
      // Give any running tasks some time to complete before forcing a shutdown
      if (!bucketManagerExecService.awaitTermination(shutdownTimeout, TimeUnit.MILLISECONDS))
        bucketManagerExecService.shutdownNow();
    }
    catch (InterruptedException e)
    {
      // We may have been interrupted waiting to finish. Force it to complete.
      bucketManagerExecService.shutdownNow();
    }
  }
}
