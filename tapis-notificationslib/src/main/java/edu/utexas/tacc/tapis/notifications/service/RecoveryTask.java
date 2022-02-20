package edu.utexas.tacc.tapis.notifications.service;

import edu.utexas.tacc.tapis.notifications.dao.NotificationsDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.Callable;

/*
 * Callable for processing notifications that are in recovery.
 *
 */
public final class RecoveryTask implements Callable<String>
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Tracing.
  private static final Logger log = LoggerFactory.getLogger(RecoveryTask.class);

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

  private final int bucketNum;

  // Use HK2 to inject singletons
  @Inject
  private NotificationsDao dao;

  /* ********************************************************************** */
  /*                             Constructors                               */
  /* ********************************************************************** */

  /*
   * Default constructor
   */
  RecoveryTask(int bucketNum1)
  {
    bucketNum = bucketNum1;
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
    log.info("**** Starting notification delivery recovery for bucket number: {}", bucketNum);
    // TODO
    try
    {
      Thread.sleep(3000000);
    }
    catch (InterruptedException e)
    {
      log.info("Notification delivery recovery interrupted. Bucket number: {}", bucketNum);
    }
//    // Wait for and process items until we are interrupted
//    Delivery delivery;
//    try
//    {
//      delivery = deliveryQueue.take();
//      processDelivery(delivery);
//    }
//    catch (IOException | InterruptedException e)
//    {
//      // TODO
//      log.warn("Caught exception: " + e.getMessage(), e);
//    }
//
    log.info("**** Stopping notification delivery recovery for bucket number: {}", bucketNum);
    return "shutdown";
  }


  /* ********************************************************************** */
  /*                             Accessors                                  */
  /* ********************************************************************** */

//  public int getBucketNum() { return bucketNum; }

  /* ********************************************************************** */
  /*                             Private Methods                            */
  /* ********************************************************************** */

//  private void processDelivery(Delivery delivery) throws IOException
//  {
//    // TODO Check to see if we have already processed this event
//    log.info("Checking for duplicate event {}", delivery.getEvent().getUuid());
//    // TODO Find matching subscriptions
//    log.info("Checking for subscriptions {}", delivery.getEvent().getUuid());
//    // TODO Create notifications from subscriptions
//    log.info("Creating notifications {}", delivery.getEvent().getUuid());
//    // TODO Persist notifications to DB
//    log.info("Persisting notifications to DB {}", delivery.getEvent().getUuid());
//
//    // All notifications for the event have been persisted, remove message from message broker queue
//    log.info("Acking event {}", delivery.getEvent().getUuid());
//    MessageBroker.getInstance().ackMsg(delivery.getDeliveryTag());
//
//    // TODO Deliver notifications (threadpool?)
//    log.info("Delivering notifications {}", delivery.getEvent().getUuid());
//    // TODO Remove notifications from DB (handled by workers in threadpool?)
//    log.info("Removing notifications from DB {}", delivery.getEvent().getUuid());
//  }
}