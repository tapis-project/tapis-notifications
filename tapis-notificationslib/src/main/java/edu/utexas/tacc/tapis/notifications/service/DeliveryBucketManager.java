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
import java.util.concurrent.LinkedBlockingQueue;

import edu.utexas.tacc.tapis.notifications.model.Event;
import edu.utexas.tacc.tapis.notifications.model.Notification;
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
  private final BlockingQueue<Delivery> deliveryQueue;

  // In-memory queues used to pass notifications to delivery workers
  private final List<BlockingQueue<Notification>> deliveryWorkerQueues = new ArrayList<>();

  // DeliveryWorkers for processing notifications
  private final List<Callable<String>> deliveryWorkers = new ArrayList<>();

  // ExecutorService and futures for delivery workers
  private final ExecutorService deliveryWorkerExecService =  Executors.newFixedThreadPool(NUM_DELIVERY_WORKERS);
  private List<Future<String>> deliveryWorkerFutures;

  /* ********************************************************************** */
  /*                             Constructors                               */
  /* ********************************************************************** */

  /*
   * Callable is associated with a specific bucket
   * deliveryQueues must be non-null and be large enough to have an entry at index bucketNum.
   */
  DeliveryBucketManager(List<BlockingQueue<Delivery>> deliveryQueues, int bucketNum1)
  {
    // Check for invalid parameters.
    if (deliveryQueues == null || bucketNum1+1 > deliveryQueues.size())
    {
      throw new IllegalArgumentException("deliveryQueues was null or too small for bucketNum: " + bucketNum1);
    }
    bucketNum = bucketNum1;
    deliveryQueue = deliveryQueues.get(bucketNum);

    // Create in-memory queues and callables for multi-threaded processing of notifications
    for (int i = 0; i < NUM_DELIVERY_WORKERS; i++)
    {
      deliveryWorkerQueues.add(new LinkedBlockingQueue<>());
      deliveryWorkers.add(new DeliveryWorker(deliveryWorkerQueues, bucketNum1, i));
    }
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

    // Wait for and process items until we are interrupted
    Delivery delivery;
    try
    {
      while (true)
      {
        delivery = deliveryQueue.take();
        processDelivery(delivery);
      }
    }
    catch (IOException | InterruptedException e)
    {
      // TODO
      log.warn("Caught exception: " + e.getMessage(), e);
    }

    log.info("**** Stopping Delivery Bucket Manager for bucket: {}", bucketNum);
    return "Delivery Bucket Manager shutdown for bucket: " + bucketNum;
  }


  /* ********************************************************************** */
  /*                             Accessors                                  */
  /* ********************************************************************** */

  public int getBucketNum() { return bucketNum; }

  /* ********************************************************************** */
  /*                             Private Methods                            */
  /* ********************************************************************** */

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
    // TODO Create notifications from subscriptions
    log.info("Creating notifications {}", event.getUuid());
    // TODO Persist notifications to DB
    log.info("Persisting notifications to DB {}", event.getUuid());

    // All notifications for the event have been persisted, remove message from message broker queue
    log.info("Acking event {}", event.getUuid());
    MessageBroker.getInstance().ackMsg(delivery.getDeliveryTag());

    // TODO Deliver notifications using an ExecutorService.
    log.info("Delivering notifications {}", event.getUuid());
    // TODO Remove notifications from DB (handled by callables in threadpool?)
    log.info("Removing notifications from DB {}", event.getUuid());
  }
}