package edu.utexas.tacc.tapis.notifications.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.utexas.tacc.tapis.notifications.model.DeliveryMethod;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.notifications.model.Event;
import edu.utexas.tacc.tapis.notifications.model.Notification;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
import edu.utexas.tacc.tapis.notifications.model.Delivery;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDao;
import static edu.utexas.tacc.tapis.notifications.service.DispatchService.NUM_DELIVERY_WORKERS;

/*
 * Callable for sending out notifications when an event is received and assigned to a bucket.
 *
 * Each callable works on a queue associated with a bucket.
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
  private final ExecutorService deliveryTaskExecService =  Executors.newFixedThreadPool(NUM_DELIVERY_WORKERS);
  private final List<Future<String>> deliveryTaskFutures = new ArrayList<>();

  // ExecutorService and future for the recovery task
  private final ExecutorService recoveryExecService = Executors.newSingleThreadExecutor();
  private Future<String> recoveryTaskFuture;

  /* ********************************************************************** */
  /*                             Constructors                               */
  /* ********************************************************************** */

  /*
   * Callable is associated with a specific bucket
   * deliveryBucketQueue must be non-null.
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

    // Start a thread to work on notifications that are in recovery.
    startRecoveryTask();

    // Process any deliveries that were interrupted during a crash.
    proccessInterruptedDeliveries();

    // Wait for and process items until we are interrupted
    Delivery delivery;
    try
    {
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
    // TODO Remove most info messages
    log.info("Processing event. Bucket: {} DeliveryTag: {} Event: {}", bucketNum, delivery.getDeliveryTag(), event);
    try { log.info("Sleep 2 seconds"); Thread.sleep(2000); } catch (InterruptedException e) {}
    if (log.isTraceEnabled())
    {
      log.trace("Processing event. Bucket: {} DeliveryTag: {} Event: {}", bucketNum, delivery.getDeliveryTag(), event);
    }

    // TODO Check to see if we have already processed this event
    log.info("Checking for duplicate. Bucket: {} Event {}", bucketNum, event.getUuid());
    try { log.info("Sleep 2 seconds"); Thread.sleep(2000); } catch (InterruptedException e) {}

    // TODO Find matching subscriptions
    log.info("Checking for subscriptions. Bucket: {} eventUUID: {}", bucketNum, event.getUuid());
    try { log.info("Sleep 2 seconds"); Thread.sleep(2000); } catch (InterruptedException e) {}
    List<Subscription> matchingSubscriptions = getMatchingSubscriptions(event);

    log.info("Number of subscriptions found: {} eventUUID: {} ", matchingSubscriptions.size(), event.getUuid());
    try { log.info("Sleep 2 seconds"); Thread.sleep(2000); } catch (InterruptedException e) {}

    // Generate and persist notifications based on subscriptions
    log.info("Creating and persisting notifications. Bucket: {} Event: {}", bucketNum, event.getUuid());
    List<Notification> notifications = createAndPersistNotifications(event, matchingSubscriptions);

    log.info("Number of notifications generated. Bucket: {} Number: {} eventUUID: {} ", bucketNum, notifications.size(), event.getUuid());
    try { log.info("Sleep 2 seconds"); Thread.sleep(2000); } catch (InterruptedException e) {}

    // All notifications for the event have been persisted, remove message from message broker queue
    log.info("Acking event {}", event.getUuid());
    try { log.info("Sleep 2 seconds"); Thread.sleep(2000); } catch (InterruptedException e) {}
    MessageBroker.getInstance().ackMsg(delivery.getDeliveryTag());

    // TODO Deliver notifications using an ExecutorService.
    log.info("Number of notifications found: {} for event: {}", notifications.size(), event.getUuid());
    try { log.info("Sleep 2 seconds"); Thread.sleep(2000); } catch (InterruptedException e) {}
    for (Notification notification : notifications)
    {
      log.info("Delivering notification for event: {} deliveryMethod: {}", event.getUuid(), notification.getDeliveryMethod());
      try { log.info("Sleep 2 seconds"); Thread.sleep(2000); } catch (InterruptedException e) {}
      Future<String> future = deliveryTaskExecService.submit(new DeliveryTask(notification, bucketNum));
      deliveryTaskFutures.add(future);
    }

    // Wait for all tasks to finish
    log.info("Waiting for queued notifications to finish. Number queued: {}", deliveryTaskFutures.size());

    // Loop indefinetely waiting for tasks to finish
    boolean notDone = true;
    while (notDone)
    {
      notDone = false;
      // Check each task. If any have not finished then notDone ends up true
      for (Future<String> f : deliveryTaskFutures)
      {
        // If task is done then log the return value, else reset notDone to true
        // TODO: This could log return values multiple times.
        if (f.isDone())
        {
          try { log.info("Bucket {} A DeliveryTask is done. Return value: {}", bucketNum, f.get()); }
          catch (ExecutionException | InterruptedException e) {}
        }
        else
        {
          notDone = true;
        }
      }
      // Pause briefly
      try
      {
        log.info("Sleep 2 seconds while waiting on delivery tasks. Bucket: {}", bucketNum);
        Thread.sleep(2000);
      }
      catch (InterruptedException e) {}
    }

    // Clear out futures
    deliveryTaskFutures.clear();

    // TODO/TBD Remove notifications from DB (or is this handled by callables in threadpool?, vis a vis recovery?)
    log.info("Removing notifications from DB {}", event.getUuid());
  }

  /*
   * TODO Check for and process an interrupted delivery
   *  An abnormal shutdown may have left us in the middle of a delivery
   */
  private void proccessInterruptedDeliveries()
  {
    // TODO
    log.info("Checking for interrupted deliveries. Bucket number: {}", bucketNum);
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
   * Get all subscriptions matching the event
   */
  private List<Subscription> getMatchingSubscriptions(Event event) throws TapisException
  {
    // TODO - for now, get all subscriptions
    log.info("Getting subscriptions. Bucket number: {}", bucketNum);
    return dao.getSubscriptions(event.getTenantId(), null, null, null, -1, null, 0, null);
  }

  /*
   * Create and persist notifications given an event and a list of matching subscriptions
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