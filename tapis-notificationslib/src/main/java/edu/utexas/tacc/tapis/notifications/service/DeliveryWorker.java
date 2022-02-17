package edu.utexas.tacc.tapis.notifications.service;

import javax.inject.Inject;

import edu.utexas.tacc.tapis.notifications.model.Delivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.notifications.dao.NotificationsDao;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/*
 * Worker thread for sending out notifications when an event is received.
 *
 * Each thread works on a queue associated with a bucket.
 * Number and types of delivery notifications will be determined by subscriptions for the event.
 *
 */
public final class DeliveryWorker extends Thread
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Tracing.
  private static final Logger log = LoggerFactory.getLogger(DeliveryWorker.class);

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

  /* ********************************************************************** */
  /*                             Constructors                               */
  /* ********************************************************************** */

  /*
   * Worker is associated with a specific bucket
   * deliveryQueues must be non-null and be large enough to have an entry at index bucketNum.
   */
  DeliveryWorker(List<BlockingQueue<Delivery>> deliveryQueues, int bucketNum1)
  {
    // Check for invalid parameters.
    if (deliveryQueues == null || bucketNum1+1 > deliveryQueues.size())
    {
      throw new IllegalArgumentException("deliveryQueues was null or too small for bucketNum: " + bucketNum1);
    }
    bucketNum = bucketNum1;
    deliveryQueue = deliveryQueues.get(bucketNum);
  }
  
  /* ********************************************************************** */
  /*                             Public Methods                             */
  /* ********************************************************************** */

  /*
   * Main method for thread.start
   */
  @Override
  public void run()
  {
    log.info("**** Starting Notifications Delivery Worker thread for bucket: {}", bucketNum);

    // Wait for and process items until we are interrupted
    Delivery delivery;
    try
    {
      delivery = deliveryQueue.take();
      processDelivery(delivery);
    }
    catch (IOException | InterruptedException e)
    {
      // TODO
      log.warn("Caught exception: " + e.getMessage(), e);
    }

    log.info("**** Stopping Notifications Delivery Worker for bucket: {}", bucketNum);
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
    // TODO Check to see if we have already processed this event
    log.info("Checking for duplicate event {}", delivery.getEvent().getUuid());
    // TODO Find matching subscriptions
    log.info("Checking for subscriptions {}", delivery.getEvent().getUuid());
    // TODO Create notifications from subscriptions
    log.info("Creating notifications {}", delivery.getEvent().getUuid());
    // TODO Persist notifications to DB
    log.info("Persisting notifications to DB {}", delivery.getEvent().getUuid());

    // All notifications for the event have been persisted, remove message from message broker queue
    log.info("Acking event {}", delivery.getEvent().getUuid());
    MessageBroker.getInstance().ackMsg(delivery.getDeliveryTag());

    // TODO Deliver notifications (threadpool?)
    log.info("Delivering notifications {}", delivery.getEvent().getUuid());
    // TODO Remove notifications from DB (handled by workers in threadpool?)
    log.info("Removing notifications from DB {}", delivery.getEvent().getUuid());
  }
}