package edu.utexas.tacc.tapis.notifications.service;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.notifications.config.RuntimeParameters;
import edu.utexas.tacc.tapis.notifications.model.DeliveryTarget;
import edu.utexas.tacc.tapis.notifications.utils.LibUtils;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.notifications.model.Event;
import edu.utexas.tacc.tapis.notifications.model.Notification;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
import edu.utexas.tacc.tapis.notifications.model.Delivery;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDao;

/*
 * Callable for sending out notifications when an event is received and assigned to a bucket.
 * Designed to run until interrupted.
 *
 * The callable works off an in-memory queue associated with a bucket.
 * Number and types of delivery notifications will be determined by subscriptions for the event.
 *
 */
public final class DeliveryBucketManager implements Callable<String>
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Logging
  private static final Logger log = LoggerFactory.getLogger(DeliveryBucketManager.class);

  // Allow interrupt when shutting down executor services.
  private static final boolean mayInterruptIfRunning = true;
  // Value returned from the call() method
  private static final String SHUTDOWN_MSG ="shutdown";
  // How long to pause on error (in minutes)
  private static final int BUCKET_ERR_PAUSE_INTERVAL = 10;

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */

  private final NotificationsDao dao;

  private final int bucketNum;
  private final BlockingQueue<Delivery> deliveryBucketQueue;

  // ExecutorService for delivery worker tasks
  private final ExecutorService deliveryTaskExecService;
  // List of futures for delivery worker tasks
  private final List<Future<Notification>> deliveryTaskFutures = new ArrayList<>();
  // Map of notifications and their future values. Used to track which notifications have been delivered
  private final Map<Future<Notification>, Notification> deliveryTaskReturns = new HashMap<>();

  // ExecutorService and future for the long-running background recovery task
  private final ExecutorService recoveryExecService = Executors.newSingleThreadExecutor();
  private Future<String> recoveryTaskFuture;

  /* ********************************************************************** */
  /*                             Constructors                               */
  /* ********************************************************************** */

  /*
   * Callable is associated with a specific bucket.
   * Dao and deliveryBucketQueue must be non-null.
   */
  DeliveryBucketManager(NotificationsDao dao1, BlockingQueue<Delivery> deliveryBucketQueue1, int bucketNum1)
  {
    // Check for invalid parameters.
    if (deliveryBucketQueue1 == null)
    {
      throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_DSP_BUCKETMGR_NULL",bucketNum1, "DeliveryBucketQueue"));
    }
    if (dao1 == null)
    {
      throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_DSP_BUCKETMGR_NULL",bucketNum1, "Dao"));
    }
    dao = dao1;
    bucketNum = bucketNum1;
    deliveryBucketQueue = deliveryBucketQueue1;
    // NOTE: Can also pass in a custom ThreadFactory if we need more control of the threads created, such
    //       as setting the thread names or possibly handling exceptions
    deliveryTaskExecService =  Executors.newFixedThreadPool(RuntimeParameters.getInstance().getNtfDeliveryThreadPoolSize());
  }
  
  /* ********************************************************************** */
  /*                             Public Methods                             */
  /* ********************************************************************** */

  /*
   * Main method for thread.start
   */
  @Override
  public String call()
  {
    Thread.currentThread().setName("Bucket-"+ bucketNum);
    log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_START", bucketNum, Thread.currentThread().getId(), Thread.currentThread().getName()));

    // RECOVERY Start a thread to work on notifications associated with this bucket that are in recovery.
    startRecoveryTask();

    // From here on we should only shut down on interrupt
    // Use an encompassing try/catch to handle errors and wait for interrupt
    boolean done = false;
    while (!done)
    {
      try
      {
        // RECOVERY Check for and process an interrupted delivery. This can happen if we crash during a delivery.
        proccessInterruptedDelivery();

        // RECOVERY Blocking call to get first event.
        // First event may be a duplicate so handle it as a special case.
        processFirstEvent();

        // Now processes events as they come in
        log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_WAIT_NEXT", bucketNum));
        Delivery delivery;
        // Loop forever until interrupted or error
        while (true)
        {
          // Blocking call to get next event
          delivery = deliveryBucketQueue.take();
          processDelivery(delivery);
        }
      }
      catch (InterruptedException e)
      {
        // We were interrupted,  it is time to stop
        log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_INTRPT", bucketNum));
        done = true;
      }
      catch (Exception e)
      {
        // Main processing loop has thrown an exception that we might be able to recover from, e.g. the DB is down.
        // Most likely this is IOException or TapisException, but catch all exceptions so we can keep going.
        // Pause for a while before resuming operations. If pause interrupted then we are done.
        log.error(LibUtils.getMsg("NTFLIB_DSP_BUCKET_ERR", bucketNum, BUCKET_ERR_PAUSE_INTERVAL, e.getMessage()), e);
        done = pauseProcessing();
      }
    }

    // We are done
    stopRecoveryTask();
    log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_STOP", bucketNum, Thread.currentThread().getId(), Thread.currentThread().getName()));
    return SHUTDOWN_MSG;
  }

  /* ********************************************************************** */
  /*                             Private Methods                            */
  /* ********************************************************************** */

  /*
   * Normal processing for a single event delivery
   */
  private void processDelivery(Delivery delivery) throws IOException, TapisException
  {
    Event event = delivery.getEvent();

    log.debug(LibUtils.getMsg("NTFLIB_DSP_BUCKET_EVENT", bucketNum, delivery.getDeliveryTag(), event));

    // Find matching subscriptions
    List<Subscription> matchingSubscriptions = dao.getSubscriptionsForEvent(event);
    log.debug(LibUtils.getMsg("NTFLIB_DSP_BUCKET_SUBS", bucketNum, event.getUuid(), matchingSubscriptions.size()));

    // Generate and persist notifications based on subscriptions, update last_event table.
    // This should all happen in a single transaction.
    List<Notification> notifications = createAndPersistNotifications(event, matchingSubscriptions);

    // RECOVERY NOTE: If we crash here, notifications will have been persisted but the event will not have been
    //    ack'd off the message queue. Hence, the check above in the call() method for a duplicate event.

    // All notifications for the event have been persisted, remove message from message broker queue
    log.debug(LibUtils.getMsg("NTFLIB_DSP_BUCKET_ACK_EVENT", bucketNum, event.getUuid()));
    MessageBroker.getInstance().ackMsg(delivery.getDeliveryTag());

    // Deliver notifications using an ExecutorService. Wait for delivery tasks to complete.
    deliverNotifications(notifications);
  }

  /*
   * Check for and process an interrupted delivery
   * An abnormal shutdown may have left us in the middle of a delivery.
   */
  private void proccessInterruptedDelivery() throws TapisException
  {
    log.debug(LibUtils.getMsg("NTFLIB_DSP_BUCKET_PROC_INT", bucketNum));
    // Get all notifications that were in progress and not yet delivered
    var notifications = dao.getNotifications(bucketNum);
    // Deliver them
    deliverNotifications(notifications);
  }

  /*
   * Wait for and process the first incoming event
   */
  private void processFirstEvent() throws TapisException, InterruptedException, IOException
  {
    log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_WAIT_FIRST", bucketNum));
    // Blocking call to get next event
    Delivery delivery = deliveryBucketQueue.take();
    // For first event received check for a duplicate. If already processed then simply ack it, else process it.
    if (dao.checkForLastEvent(delivery.getEvent().getUuid(), bucketNum))
    {
      log.warn(LibUtils.getMsg("NTFLIB_DSP_BUCKET_ACK_DUP", bucketNum, delivery.getEvent().getUuid()));
      MessageBroker.getInstance().ackMsg(delivery.getDeliveryTag());
    }
    else
    {
      processDelivery(delivery);
    }
  }

  /*
   * Start the thread for processing notifications in recovery
   */
  private void startRecoveryTask()
  {
    log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_START_RCVRY", bucketNum));
    recoveryTaskFuture = recoveryExecService.submit(new RecoveryTask(bucketNum, dao));
  }

  /*
   * Stop the thread for processing notifications in recovery
   */
  public void stopRecoveryTask()
  {
    log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_STOP_RCVRY", bucketNum));
    if (recoveryTaskFuture != null)  recoveryTaskFuture.cancel(mayInterruptIfRunning);
  }

  /*
   * Create and persist notifications given an event and a list of matching subscriptions
   * Also update the last_event table as part of the transaction.
   */
  private List<Notification> createAndPersistNotifications(Event event, List<Subscription> subscriptions)
          throws TapisException
  {
    var notifList = new ArrayList<Notification>();
    if (event == null || subscriptions == null || subscriptions.isEmpty()) return notifList;

    log.debug(LibUtils.getMsg("NTFLIB_DSP_BUCKET_GEN_N1", bucketNum, event.getUuid()));

    String tenant = event.getTenant();
    UUID eventUuid = event.getUuid();

    // For each deliveryMethod in a Subscription
    for (Subscription s : subscriptions)
    {
      var deliveryMethods = s.getDeliveryTargets();
      if (deliveryMethods == null || deliveryMethods.isEmpty()) continue;
      for (DeliveryTarget dm : deliveryMethods)
      {
        Instant created = TapisUtils.getUTCTimeNow().toInstant(ZoneOffset.UTC);
        notifList.add(new Notification(null, s.getSeqId(), tenant, s.getName(), bucketNum, eventUuid, event, dm, created));
      }
    }
    // Persist all notifications and update the last_event table in a single transaction.
    dao.persistNotificationsAndUpdateLastEvent(event.getTenant(), event, bucketNum, notifList);
    log.debug(LibUtils.getMsg("NTFLIB_DSP_BUCKET_GEN_N2", bucketNum, event.getUuid(), notifList.size()));
    return notifList;
  }

  /*
   * Deliver notifications using an ExecutorService.
   * Wait for all delivery tasks to complete.
   */
  private void deliverNotifications(List<Notification> notifications)
  {
    // If nothing to do then return
    if (notifications == null || notifications.isEmpty()) return;

    // Get eventUuid for logging. Each notification has the same event
    UUID eventUuid = notifications.get(0).getEventUuid();

    // Clear out futures list and map. They get re-used.
    deliveryTaskFutures.clear();
    deliveryTaskReturns.clear();
    // Add a delivery task for each notification
    for (Notification ntf : notifications)
    {
      log.debug(LibUtils.getMsg("NTFLIB_DSP_BUCKET_DLVRY1", bucketNum, ntf.getEventUuid(), ntf.getDeliveryMethod()));
      // Create a delivery task and submit it to the executor service.
      Future<Notification> future = deliveryTaskExecService.submit(new DeliveryTask(dao, ntf));
      // Add the task to the list
      deliveryTaskFutures.add(future);
      // Initialize the map entry for tracking the future returns
      deliveryTaskReturns.put(future, null);
    }

    // Wait for all tasks to finish
    log.debug(LibUtils.getMsg("NTFLIB_DSP_BUCKET_DLVRY2", bucketNum, eventUuid, deliveryTaskFutures.size()));
    // Loop indefinitely waiting for tasks to finish
    // The call to get a future value for a task is a blocking call so no need to pause.
    // As tasks finish the future values get put into the map. Once all tasks are done the final pass through the
    //   list of futures will quickly fill in any remaining values and the loop will exit.
    boolean notDone = true;
    while (notDone)
    {
      notDone = false;
      // Check each task and capture return values when available.
      // If any have not finished then notDone ends up true
      for (Future<Notification> f : deliveryTaskFutures)
      {
        // If task is not done then reset notDone to true, else record and log the return value
        if (!f.isDone())
        {
          notDone = true;
        }
        else
        {
          // Task is done. If we have not captured the return value do it now.
          // Note that the Future.get() will throw an InterruptedException or ExecutionException if the underlying
          //   thread threw an exception, including runtime exceptions.
          if (deliveryTaskReturns.get(f) == null)
          {
            try
            {
              // Make a blocking call to get the return value of the future.
              Notification ret = f.get();
              log.debug(LibUtils.getMsg("NTFLIB_DSP_BUCKET_DLVRY3", bucketNum, eventUuid, ret.getDeliveryMethod()));
              deliveryTaskReturns.put(f, ret);
            }
            catch (InterruptedException e)
            {
              // Log exception for the failed delivery
              log.warn(LibUtils.getMsg("NTFLIB_DSP_BUCKET_DLVRY_ERR1", bucketNum, eventUuid, e.getMessage()), e);
            }
            catch (ExecutionException e)
            {
              // Log exception for the failed delivery
              log.warn(LibUtils.getMsg("NTFLIB_DSP_BUCKET_DLVRY_ERR2", bucketNum, eventUuid, e.getMessage()), e);
            }
          }
        }
      }
    }

    // At this point all tasks are done. The map of values has been filled in as much as possible.
    //   If any tasks threw an exception then the value in the map deliveryTaskReturns will be null.

  }

  /**
   * Pause for given number of minutes for a process
   * @return true if interrupted, else false
   */
  private boolean pauseProcessing()
  {
    try
    {
      Thread.sleep(BUCKET_ERR_PAUSE_INTERVAL * 60L * 1000L);
    }
    catch (InterruptedException e)
    {
      log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_INTRPT", bucketNum));
      return true;
    }
    return false;
  }
}