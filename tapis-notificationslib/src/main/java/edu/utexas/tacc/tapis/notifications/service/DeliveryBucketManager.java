package edu.utexas.tacc.tapis.notifications.service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.utexas.tacc.tapis.notifications.model.DeliveryMethod;
import edu.utexas.tacc.tapis.notifications.model.Event;
import edu.utexas.tacc.tapis.notifications.model.Notification;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  // Tracing.
  private static final Logger log = LoggerFactory.getLogger(DeliveryBucketManager.class);

  // Allow interrupt when shutting down executor services.
  private static final boolean mayInterruptIfRunning = true;

//  public static final String VHOST = "NotificationsHost";
//  public static final String DEFAULT_BINDING_KEY = "#";
//  public static final String EXCHANGE_MAIN = "tapis.notifications.exchange";
//  public static final String QUEUE_MAIN = "tapis.notifications.queue";

  /* ********************************************************************** */
  /*                                Enums                                   */
  /* ********************************************************************** */

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */

  // Use HK2 to inject singletons
  @Inject
  private NotificationsDao dao;

  private final int bucketNum;
  private final BlockingQueue<Delivery> deliveryBucketQueue;

  // ExecutorService and futures for delivery workers
  private final ExecutorService deliveryTaskExecService =  Executors.newFixedThreadPool(NUM_DELIVERY_WORKERS);
  private List<Future<String>> deliveryTaskFutures = new ArrayList<>();

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
  DeliveryBucketManager(BlockingQueue<Delivery> deliveryBucketQueue1, int bucketNum1)
  {
    // Check for invalid parameters.
    if (deliveryBucketQueue1 == null)
    {
      throw new IllegalArgumentException("deliveryBucketQueue was null for bucketNum: " + bucketNum1);
    }
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
    log.info("**** Starting Delivery Bucket Manager for bucket number: {}", bucketNum);

    // Start our recovery thread.
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
      log.info("**** Delivery Bucket Manager interrupted. Bucket number: {}", bucketNum);
    }
    catch (IOException e)
    {
      // TODO
      log.warn("Caught exception for bucket manager. Bucket number: {} Exception: {}", bucketNum, e.getMessage());
    }

    log.info("**** Stopping Delivery Bucket Manager for bucket: {}", bucketNum);

    stopRecoveryTask();

    return "Delivery Bucket Manager shutdown for bucket: " + bucketNum;
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
  private void processDelivery(Delivery delivery) throws IOException
  {
    Event event = delivery.getEvent();
    log.info("Processing event. DeliveryTag: {} Source: {} Type: {} Subject: {} SeriesId: {} Time: {} UUID {}",
             delivery.getDeliveryTag(), event.getSource(), event.getType(), event.getSubject(), event.getSeriesId(),
             event.getTime(), event.getUuid());

    // TODO Check to see if we have already processed this event
    log.info("Checking for duplicate event {}", event.getUuid());

    // TODO Find matching subscriptions
    log.info("Checking for subscriptions {}", event.getUuid());
//    List<Subscription> matchingSubscriptions = dao.getMatchingSubscriptions(???);
    List<Subscription> matchingSubscriptions = new ArrayList<>();
//  public Subscription(int seqId1, String tenant1, String id1, String description1, String owner1, boolean enabled1,
//    String typeFilter1, String subjectFilter1, List< DeliveryMethod > dmList1, int ttl1, Object notes1,
//          UUID uuid1, Instant expiry1, Instant created1, Instant updated1)

    // TODO test using a single subscription with one delivery method
    DeliveryMethod dm1 = new DeliveryMethod(Subscription.DeliveryType.EMAIL, "test1@example.com");
    List<DeliveryMethod> dmList1 = new ArrayList<>(List.of(dm1));
    Subscription sub1 = new Subscription(-1, "dev", "test-sub1", null, "testuser2", true, "*", "*", dmList1, -1,
                                         null, null, null, null, null);
    matchingSubscriptions.add(sub1);

    log.info("Number of subscriptions found: {}", matchingSubscriptions.size());

    // TODO Create notifications from subscriptions
    log.info("Creating notifications {}", event.getUuid());
    List<Notification> notifications = new ArrayList<>();// createNotifications(matchingSubscriptions);
    Notification notif1 = new Notification(event, dm1, sub1, bucketNum, 1, null, null, null, null);
    notifications.add(notif1);

    // TODO Persist notifications to DB
    log.info("Persisting notifications to DB {}", event.getUuid());

    // All notifications for the event have been persisted, remove message from message broker queue
    log.info("Acking event {}", event.getUuid());
    MessageBroker.getInstance().ackMsg(delivery.getDeliveryTag());

    // TODO Deliver notifications using an ExecutorService.
    log.info("Number of notifications found: {} for event: {}", notifications.size(), event.getUuid());
    for (int i = 0;  i < notifications.size(); i++)
    {
      Future<String> future =  deliveryTaskExecService.submit(new DeliveryTask(notifications.get(i), bucketNum));
      deliveryTaskFutures.add(future);
    }

    // TODO/TBD: Wait for all tasks to finish? Or continue so one event does not block next event?
    log.info("Number of notifications queued for processing: {}", deliveryTaskFutures.size());

    // Clear out futures
    deliveryTaskFutures.clear();

    // TODO/TBD Remove notifications from DB (or is this handled by callables in threadpool?)
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
    recoveryTaskFuture = recoveryExecService.submit(new RecoveryTask(bucketNum));
  }

  /*
   * Stop the thread for processing notifications in recovery
   */
  public void stopRecoveryTask()
  {
    log.info("Stopping Recovery task for bucket number: {}", bucketNum);
    recoveryTaskFuture.cancel(mayInterruptIfRunning);
  }
}