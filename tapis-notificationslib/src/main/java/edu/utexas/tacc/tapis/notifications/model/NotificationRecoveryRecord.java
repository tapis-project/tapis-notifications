package edu.utexas.tacc.tapis.notifications.model;

import java.time.Instant;

/*
 * A NotificationRecoveryRecord represents an in-flight notification that has encountered problems during delivery.
 *
 *
 * This class is intended to represent an immutable object.
 * Please keep it immutable.
 */
public final class NotificationRecoveryRecord
{
  // The notification being delivered
  private final Notification notification;
  // Recovery related attributes
  private final int recoveryAttemptNum;
  private final Instant lastRecoveryAttempt;
  private final Instant recoveryExpiry;
  private Instant created; // UTC time for when record was created
  private Instant updated; // UTC time for when record was last updated

  public NotificationRecoveryRecord(Notification notification1, int recoveryAttemptNum1, Instant lastRecoveryAttempt1,
                                    Instant recoveryExpiry1, Instant created1, Instant updated1)
  {
    notification = notification1;
    recoveryAttemptNum = recoveryAttemptNum1;
    lastRecoveryAttempt = lastRecoveryAttempt1;
    recoveryExpiry = recoveryExpiry1;
    created = created1;
    updated = updated1;
  }

  public Notification getNotification() { return notification; }
  public int getRecoveryAttemptNum() { return recoveryAttemptNum; }
  public Instant getLastRecoveryAttempt() { return lastRecoveryAttempt; }
  public Instant getRecoveryExpiry() { return recoveryExpiry; }
  public Instant getCreated() { return created; }
  public Instant getUpdated() { return updated; }
}
