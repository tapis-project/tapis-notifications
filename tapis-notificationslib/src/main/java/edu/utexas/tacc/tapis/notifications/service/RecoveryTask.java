package edu.utexas.tacc.tapis.notifications.service;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDao;

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

  /* ********************************************************************** */
  /*                                Enums                                   */
  /* ********************************************************************** */

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */

  private final int bucketNum;

  private final NotificationsDao dao;

  /* ********************************************************************** */
  /*                             Constructors                               */
  /* ********************************************************************** */

  /*
   * Default constructor
   */
  RecoveryTask(int bucketNum1, NotificationsDao dao1)
  {
    bucketNum = bucketNum1;
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
    log.info("**** Starting notification delivery recovery for bucket number: {}", bucketNum);
    Thread.currentThread().setName("Recovery-bucket-"+ bucketNum);
    log.info("ThreadId: {} ThreadName: {}", Thread.currentThread().getId(), Thread.currentThread().getName());
    // TODO
    try
    {
      log.info("TODO: For now, bucketRecovery task just sleeping ...");
      Thread.sleep(3000000);
    }
    catch (InterruptedException e)
    {
      log.info("Notification delivery recovery interrupted. Bucket number: {}", bucketNum);
    }

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

//  // TODO/TBD: This might help in supporting a callable along with afterExecute
//  //           also need a custom threadfactory?
//
//class RecoveryFutureTask<T> extends FutureTask<T>
//{
//  private Callable<T> callable;
//
//  public RecoveryFutureTask(Callable<T> callable){
//    super(callable);
//    this.callable = callable;
//  }
//
//  public Callable<T> getCallable(){
//    return this.callable;
//  }
//}
}