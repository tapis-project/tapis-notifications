package edu.utexas.tacc.tapis.notifications.service;

import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDao;

/*
 * Callable process for cleaning up expired subscriptions.
 *
 */
public final class SubscriptionReaper implements Callable<String>
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Tracing.
  private static final Logger log = LoggerFactory.getLogger(SubscriptionReaper.class);

  /* ********************************************************************** */
  /*                                Enums                                   */
  /* ********************************************************************** */

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */

  private NotificationsDao dao;

  /* ********************************************************************** */
  /*                             Constructors                               */
  /* ********************************************************************** */

  SubscriptionReaper(NotificationsDao dao1)
  {
    dao = dao1;
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
    log.info("**** Starting Subscription Reaper");
    try
    {
      // TODO
      log.info("TODO: For now, reaper only sleeps ...");
      Thread.sleep(3000000);
    }
    catch (InterruptedException e)
    {
      log.info("Subscription Reaper interrupted");
    }
    log.info("**** Stopping Subscription Reaper");
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