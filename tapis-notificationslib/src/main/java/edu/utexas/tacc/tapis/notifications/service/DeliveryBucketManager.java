package edu.utexas.tacc.tapis.notifications.service;

import java.io.IOException;
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

import edu.utexas.tacc.tapis.notifications.config.RuntimeParameters;
import edu.utexas.tacc.tapis.notifications.model.DeliveryMethod;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.notifications.model.Event;
import edu.utexas.tacc.tapis.notifications.model.Notification;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
import edu.utexas.tacc.tapis.notifications.model.Delivery;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDao;

/*
 * Callable for sending out notifications when an event is received and assigned to a bucket.
 *
 * Each callable works from an in-memory queue associated with a bucket.
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

  /* ********************************************************************** */
  /*                                Enums                                   */
  /* ********************************************************************** */

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */

  private final NotificationsDao dao;

  private final int bucketNum;
  private final BlockingQueue<Delivery> deliveryBucketQueue;

  // ExecutorService and futures for delivery workers
  private final ExecutorService deliveryTaskExecService;
  private final List<Future<Notification>> deliveryTaskFutures = new ArrayList<>();
  private final Map<Future<Notification>, Notification> deliveryTaskReturns = new HashMap<>();

  // ExecutorService and future for the long-running background recovery task
  private final ExecutorService recoveryExecService = Executors.newSingleThreadExecutor();
  private Future<String> recoveryTaskFuture;

  /* ********************************************************************** */
  /*                             Constructors                               */
  /* ********************************************************************** */

  /*
   * Callable is associated with a specific bucket
   * dao and deliveryBucketQueue must be non-null.
   */
  DeliveryBucketManager(NotificationsDao dao1, BlockingQueue<Delivery> deliveryBucketQueue1, int bucketNum1)
  {
    // Check for invalid parameters.
    if (deliveryBucketQueue1 == null)
    {
      throw new IllegalArgumentException("deliveryBucketQueue was null for bucketNum: " + bucketNum1);
    }
    if (dao1 == null)
    {
      throw new IllegalArgumentException("NotificationsDao was null for bucketNum: " + bucketNum1);
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
    log.info("**** Starting Delivery Bucket Manager for bucket: {}", bucketNum);
    Thread.currentThread().setName("Bucket-"+ bucketNum);
    log.info("ThreadId: {} ThreadName: {}", Thread.currentThread().getId(), Thread.currentThread().getName());

    // RECOVERY Start a thread to work on notifications associated with this bucket that are in recovery.
    startRecoveryTask();

    // RECOVERY Process any deliveries for this bucket that were interrupted during a crash.
    proccessInterruptedDeliveries();

    // Wait for and process items until we are interrupted
    Delivery delivery;
    try
    {
      // RECOVERY Blocking call to get first event
      // For first event received check for a duplicate. If already processed then simply ack it, else process it.
      delivery = deliveryBucketQueue.take();
      if (dao.checkForLastEvent(delivery.getEvent().getUuid(), bucketNum))
      {
        log.warn("Acking duplicate event {}", delivery.getEvent().getUuid());
        MessageBroker.getInstance().ackMsg(delivery.getDeliveryTag());
      }
      else
      {
        processDelivery(delivery);
      }

      // Now processes events as they come in
      while (true)
      {
        // Blocking call to get next event
        delivery = deliveryBucketQueue.take();
        processDelivery(delivery);
      }
    }
    catch (InterruptedException e)
    {
      log.info("**** Delivery Bucket Manager interrupted. Bucket: {}", bucketNum);
    }
    catch (IOException e)
    {
      // TODO
      log.warn("Caught IOException for bucket manager. Bucket: {} Exception: {}", bucketNum, e.getMessage());
    }
    catch (TapisException e)
    {
      // TODO
      log.warn("Caught TapisException for bucket manager. Bucket: {} Exception: {}", bucketNum, e.getMessage());
    }
    finally
    {
      stopRecoveryTask();
    }

    log.info("Delivery Bucket Manager shutdown for bucket: {}", bucketNum);
    return "shutdown";
  }


  /* ********************************************************************** */
  /*                             Accessors                                  */
  /* ********************************************************************** */

//  public int getBucketNum() { return bucketNum; }

  /* ********************************************************************** */
  /*                             Private Methods                            */
  /* ********************************************************************** */

  /*
   * Normal processing for a single event delivery
   */
  private void processDelivery(Delivery delivery) throws IOException, TapisException
  {
    Event event = delivery.getEvent();

    // TODO Clean up messages and sleeps
    // TODO/TBD: refactor into separate calls to private methods

    log.info("Processing event. Bucket: {} DeliveryTag: {} Event: {}", bucketNum, delivery.getDeliveryTag(), event);
    if (log.isTraceEnabled())
    {
      log.trace("Processing event. Bucket: {} DeliveryTag: {} Event: {}", bucketNum, delivery.getDeliveryTag(), event);
    }
    try { log.info("Sleep 2 seconds"); Thread.sleep(2000); } catch (InterruptedException e) {}

    // Find matching subscriptions
    log.info("Checking for subscriptions. Bucket: {} eventUUID: {}", bucketNum, event.getUuid());
    try { log.info("Sleep 2 seconds"); Thread.sleep(2000); } catch (InterruptedException e) {}
    List<Subscription> matchingSubscriptions = getMatchingSubscriptions(event);

    log.info("Number of subscriptions found: {} eventUUID: {} ", matchingSubscriptions.size(), event.getUuid());
    try { log.info("Sleep 2 seconds"); Thread.sleep(2000); } catch (InterruptedException e) {}

    // Generate and persist notifications based on subscriptions, update last_event table.
    log.info("Creating and persisting notifications. Bucket: {} Event: {}", bucketNum, event.getUuid());
    List<Notification> notifications = createAndPersistNotifications(event, matchingSubscriptions);

    log.info("Number of notifications generated. Bucket: {} Number: {} eventUUID: {} ", bucketNum, notifications.size(), event.getUuid());
    try { log.info("Sleep 2 seconds"); Thread.sleep(2000); } catch (InterruptedException e) {}

    // RECOVERY NOTE: If we crash here, notifications will have been persisted but the event will not have been
    //    ack'd off the message queue. Hence, the check above in the call() method for a duplicate event.

    // All notifications for the event have been persisted, remove message from message broker queue
    log.info("Acking event {}", event.getUuid());
    try { log.info("Sleep 2 seconds"); Thread.sleep(2000); } catch (InterruptedException e) {}
    MessageBroker.getInstance().ackMsg(delivery.getDeliveryTag());

    // Deliver notifications using an ExecutorService.
    log.info("Number of notifications found: {} for event: {}", notifications.size(), event.getUuid());
    try { log.info("Sleep 2 seconds"); Thread.sleep(2000); } catch (InterruptedException e) {}
    for (Notification notification : notifications)
    {
      log.info("Delivering notification for event: {} deliveryMethod: {}", event.getUuid(), notification.getDeliveryMethod());
      try { log.info("Sleep 2 seconds"); Thread.sleep(2000); } catch (InterruptedException e) {}
      Future<Notification> future = deliveryTaskExecService.submit(new DeliveryTask(dao, notification));
      deliveryTaskFutures.add(future);
      deliveryTaskReturns.put(future, null);
    }

    // Wait for all tasks to finish
    log.info("Waiting for queued notifications to finish. Number queued: {}", deliveryTaskFutures.size());
    // TODO/TBD: Refactor into private method
    // Loop indefinitely waiting for tasks to finish
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
          // TODO deal with exceptions
          if (deliveryTaskReturns.get(f) == null)
          {
            try
            {
              Notification ret = f.get();
              log.info("Bucket {}. A DeliveryTask is done. Return value: {}", bucketNum, ret.getDeliveryMethod().getDeliveryAddress());
              deliveryTaskReturns.put(f, ret);
            }
            catch (InterruptedException e)
            {
              log.error("Caught InterruptedException while trying to capture return value. Bucket: {}. Exception: {}", bucketNum, e);
            }
            catch (ExecutionException e)
            {
              log.error("Caught ExecutionException while trying to capture return value. Bucket: {}. Exception: {}", bucketNum, e);
            }
          }
        }
      }
      // Pause briefly
      try
      {
        log.info("Sleep 1 second while waiting on delivery tasks. Bucket: {}", bucketNum);
        Thread.sleep(1000);
      }
      catch (InterruptedException e) {}
    }

    // Clear out futures
    deliveryTaskFutures.clear();
  }

  /*
   * TODO Check for and process an interrupted delivery
   *  An abnormal shutdown may have left us in the middle of a delivery
   */
  private void proccessInterruptedDeliveries()
  {
    // TODO
    log.info("TODO Checking for interrupted deliveries. Bucket number: {}", bucketNum);
  }

  /*
   * Start the thread for processing notifications in recovery
   */
  private void startRecoveryTask()
  {
    log.info("Starting Recovery task for bucket number: {}", bucketNum);
    recoveryTaskFuture = recoveryExecService.submit(new RecoveryTask(bucketNum, dao));
  }

  /*
   * Stop the thread for processing notifications in recovery
   */
  public void stopRecoveryTask()
  {
    log.info("Stopping Recovery task for bucket number: {}", bucketNum);
    if (recoveryTaskFuture != null)  recoveryTaskFuture.cancel(mayInterruptIfRunning);
  }

  /*
   * Get subscriptions matching the event
   */
  private List<Subscription> getMatchingSubscriptions(Event event) throws TapisException
  {
    log.info("Getting matching subscriptions for an event. Bucket number: {}", bucketNum);
    return dao.getSubscriptionsForEvent(event);
  }

  /*
   * Create and persist notifications given an event and a list of matching subscriptions
   * Also update the last_event table as part of the transaction.
   */
  private List<Notification> createAndPersistNotifications(Event event, List<Subscription> subscriptions)
          throws TapisException
  {
    log.info("Generating notifications. Bucket number: {}", bucketNum);
    var notifList = new ArrayList<Notification>();
    if (event == null || subscriptions == null || subscriptions.isEmpty()) return notifList;

    String tenant = event.getTenantId();
    UUID eventUuid = event.getUuid();

    // For each deliveryMethod in a Subscription
    for (Subscription s : subscriptions)
    {
      var deliveryMethods = s.getDeliveryMethods();
      if (deliveryMethods == null || deliveryMethods.isEmpty()) continue;
      for (DeliveryMethod dm : deliveryMethods)
      {
        notifList.add(new Notification(-1, s.getSeqId(), tenant, bucketNum, eventUuid, event, dm, null));
      }
    }
    dao.persistNotificationsForEvent(event.getTenantId(), event, bucketNum, notifList);
    return notifList;
  }
}