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

import edu.utexas.tacc.tapis.notifications.config.RuntimeParameters;
import edu.utexas.tacc.tapis.notifications.model.DeliveryMethod;
import edu.utexas.tacc.tapis.notifications.utils.LibUtils;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
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

    // TODO RECOVERY Process any deliveries for this bucket that were interrupted during a crash.
    proccessInterruptedDeliveries();

    Delivery delivery;
    boolean done = false;

    // Wait for and process first event until we are interrupted or error
    // If interrupted we are done, on error continue.
    try
    {
      // RECOVERY Blocking call to get first event.
      // First event may be a duplicate so handle it as a special case.
      // For first event received check for a duplicate. If already processed then simply ack it, else process it.
      log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_WAIT_FIRST", bucketNum));
      delivery = deliveryBucketQueue.take();
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
    catch (InterruptedException e)
    {
      log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_INTRPT", bucketNum));
      // If interrupted waiting on first event then we are done.
      done = true;
    }
    catch (IOException e)
    {
      log.warn(LibUtils.getMsg("NTFLIB_DSP_BUCKET_ERR1", bucketNum, e.getMessage()), e);
    }
    catch (TapisException e)
    {
      log.warn(LibUtils.getMsg("NTFLIB_DSP_BUCKET_ERR2", bucketNum, e.getMessage()), e);
    }

    // Now processes events as they come in until we are interrupted or error
    // If interrupted we are done, on error continue.
    log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_WAIT_NEXT", bucketNum));
    while (!done)
    {
      try
      {
        // Blocking call to get next event
        delivery = deliveryBucketQueue.take();
        processDelivery(delivery);
      }
      catch (InterruptedException e)
      {
        log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_INTRPT", bucketNum));
        // If interrupted we are done
        done = true;
      }
      catch (IOException e)
      {
        log.warn(LibUtils.getMsg("NTFLIB_DSP_BUCKET_ERR1", bucketNum, e.getMessage()), e);
      }
      catch (TapisException e)
      {
        log.warn(LibUtils.getMsg("NTFLIB_DSP_BUCKET_ERR2", bucketNum, e.getMessage()), e);
      }
    }
    stopRecoveryTask();
    log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_STOP", bucketNum, Thread.currentThread().getId(), Thread.currentThread().getName()));
    return "shutdown";
  }


  /* ********************************************************************** */
  /*                             Accessors                                  */
  /* ********************************************************************** */


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
    List<Notification> notifications = createAndPersistNotifications(event, matchingSubscriptions);

    // RECOVERY NOTE: If we crash here, notifications will have been persisted but the event will not have been
    //    ack'd off the message queue. Hence, the check above in the call() method for a duplicate event.

    // All notifications for the event have been persisted, remove message from message broker queue
    log.debug(LibUtils.getMsg("NTFLIB_DSP_BUCKET_ACK_EVENT", bucketNum, event.getUuid()));
    MessageBroker.getInstance().ackMsg(delivery.getDeliveryTag());

    // Deliver notifications using an ExecutorService. Wait for delivery tasks to complete.
    deliverNotifications(event, notifications);
  }

  /*
   *  Check for and process an interrupted delivery
   *  An abnormal shutdown may have left us in the middle of a delivery
   *  TODO
   */
  private void proccessInterruptedDeliveries()
  {
    log.debug(LibUtils.getMsg("NTFLIB_DSP_BUCKET_PROC_INT", bucketNum));
// TODO
//  Delivery delivery = null;
//  processDelivery(delivery);
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
      var deliveryMethods = s.getDeliveryMethods();
      if (deliveryMethods == null || deliveryMethods.isEmpty()) continue;
      for (DeliveryMethod dm : deliveryMethods)
      {
        Instant created = TapisUtils.getUTCTimeNow().toInstant(ZoneOffset.UTC);
        notifList.add(new Notification(null, s.getSeqId(), tenant, s.getId(), bucketNum, eventUuid, event, dm, created));
      }
    }
    dao.persistNotificationsAndUpdateLastEvent(event.getTenant(), event, bucketNum, notifList);
    log.debug(LibUtils.getMsg("NTFLIB_DSP_BUCKET_GEN_N2", bucketNum, event.getUuid(), notifList.size()));
    return notifList;
  }

  /*
   * Deliver notifications using an ExecutorService.
   * Wait for all delivery tasks to complete.
   */
  private void deliverNotifications(Event event, List<Notification> notifications)
  {
    for (Notification notification : notifications)
    {
      log.debug(LibUtils.getMsg("NTFLIB_DSP_BUCKET_DLVRY1", bucketNum, event.getUuid(), notification.getDeliveryMethod()));
      Future<Notification> future = deliveryTaskExecService.submit(new DeliveryTask(dao, notification));
      deliveryTaskFutures.add(future);
      deliveryTaskReturns.put(future, null);
    }

    // Wait for all tasks to finish
    log.debug(LibUtils.getMsg("NTFLIB_DSP_BUCKET_DLVRY2", bucketNum, event.getUuid(), deliveryTaskFutures.size()));
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
          // TODO/TBD deal with exceptions
          if (deliveryTaskReturns.get(f) == null)
          {
            try
            {
              Notification ret = f.get();
              log.debug(LibUtils.getMsg("NTFLIB_DSP_BUCKET_DLVRY3", bucketNum, event.getUuid(), ret.getDeliveryMethod()));
              deliveryTaskReturns.put(f, ret);
            }
            catch (InterruptedException e)
            {
              log.warn(LibUtils.getMsg("NTFLIB_DSP_BUCKET_DLVRY_ERR1", bucketNum, event.getUuid(), e.getMessage()), e);
            }
            catch (ExecutionException e)
            {
              log.warn(LibUtils.getMsg("NTFLIB_DSP_BUCKET_DLVRY_ERR2", bucketNum, event.getUuid(), e.getMessage()), e);
            }
          }
        }
      }
    }

    // Clear out futures
    deliveryTaskFutures.clear();
  }
}