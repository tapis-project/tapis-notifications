package edu.utexas.tacc.tapis.notifications.service;

import java.util.List;
import java.util.concurrent.Callable;

import edu.utexas.tacc.tapis.notifications.config.RuntimeParameters;
import edu.utexas.tacc.tapis.notifications.model.Notification;
import edu.utexas.tacc.tapis.notifications.utils.LibUtils;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDao;

/*
 * Callable for processing notifications that are in recovery.
 * When the process wakes up it makes a single delivery attempt for each notification in recovery.
 * Once the maximum number of attempts for a notification is reached an error is logged and the
 * notification is removed from the recovery table.
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
  private final int sleepTime;
  private final int maxAttempts;

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
    sleepTime = RuntimeParameters.getInstance().getNtfDeliveryRecoveryRetryInterval();
    maxAttempts = RuntimeParameters.getInstance().getNtfDeliveryRecoveryMaxAttempts();
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
    Thread.currentThread().setName("Recovery-bucket-"+ bucketNum);
    log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_RCVRY_START", bucketNum, sleepTime,
                             Thread.currentThread().getId(), Thread.currentThread().getName()));
    // Convert sleepTime from minutes to milliseconds;
    long sleepTimeMs = sleepTime * 60L * 1000L;

    boolean done = false;
    // Loop until we are interrupted or error
    // If interrupted we are done, on error continue.
    while (!done)
    {
      try
      {
        // Get all notifications in recovery for our bucket
        List<Notification> notifications = dao.getNotificationsInRecovery(bucketNum);
        // Make one pass through each notification in recovery
        for (Notification ntf : notifications)
        {
          boolean delivered = DeliveryTask.deliverNotification(ntf);
          // If delivered ok we are done. Perform any post-delivery steps and return
          if (delivered)
          {
            recoveryAttemptSucceeded(ntf);
          }
          else
          {
            recoveryAttemptFailed(ntf);
          }
        }
      }
      catch (TapisException e)
      {
        log.warn(LibUtils.getMsg("NTFLIB_DSP_BUCKET_RCVRY_ERR", bucketNum, e.getMessage()), e);
      }

      // Pause for configured interval before checking for more work and trying again
      // If interrupted it is time to shut down
      log.debug(LibUtils.getMsg("NTFLIB_DSP_BUCKET_RCVRY_PAUSE", bucketNum, sleepTime));
      try
      {
        Thread.sleep(sleepTimeMs);
      }
      catch (InterruptedException e)
      {
        log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_RCVRY_INTRPT", bucketNum));
        done = true;
      }
    }
    log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_RCVRY_STOP", bucketNum, Thread.currentThread().getId(),
                             Thread.currentThread().getName()));
    return "shutdown";
  }

  /* ********************************************************************** */
  /*                             Accessors                                  */
  /* ********************************************************************** */


  /* ********************************************************************** */
  /*                             Private Methods                            */
  /* ********************************************************************** */

  /*
   * Notification has been delivered.
   * Log a msg and remove it from the table.
   */
  private void recoveryAttemptSucceeded(Notification notification)
  {
    log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_RCVRY_SUCCESS", bucketNum, notification.getUuid()));
    try { dao.deleteNotificationFromRecovery(notification); }
    catch (TapisException e)
    {
      String msg = LibUtils.getMsg("NTFLIB_DSP_BUCKET_RCVRY_DEL_ERR", bucketNum, notification.getUuid(),
                                   e.getMessage(), e);
      log.error(msg);
    }
  }

  /*
   * Notification recovery attempt failed.
   * Either update the attempt count or remove it.
   * Log a warning or error.
   */
  private void recoveryAttemptFailed(Notification notification)
  {
    try
    {
      // Get the number of attempts
      int numAttempts = dao.getNotificationRecoveryAttemptCount(notification);
      // if we hit the max then log and error and remove it
      // else log a warning and bump up the count
      if (numAttempts >= maxAttempts)
      {
        log.error(LibUtils.getMsg("NTFLIB_DSP_BUCKET_RCVRY_FAIL_MAX", bucketNum, notification.getUuid(), maxAttempts));
        dao.deleteNotificationFromRecovery(notification);
      }
      else
      {
        log.warn(LibUtils.getMsg("NTFLIB_DSP_BUCKET_RCVRY_FAIL", bucketNum, notification.getUuid(), numAttempts));
        dao.setNotificationRecoveryAttemptCount(notification, numAttempts++);
      }
    }
    catch (TapisException e)
    {
      String msg = LibUtils.getMsg("NTFLIB_DSP_BUCKET_RCVRY_FAIL_ERR", bucketNum, notification.getUuid(),
                                   e.getMessage(), e);
      log.error(msg);
    }
  }
}