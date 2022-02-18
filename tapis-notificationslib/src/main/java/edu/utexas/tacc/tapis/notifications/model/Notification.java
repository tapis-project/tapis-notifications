package edu.utexas.tacc.tapis.notifications.model;

import java.time.Instant;

/*
 * A Notification is a notice to a subscriber that an event has occurred.
 *
 * When an event is received it can result in 0 or more notifications since there may be 0 or more matching
 *   subscriptions and each subscription has 1 or more delivery methods.
 *   Each delivery method in a matching subscription will result in a notification.
 * Notifications are created by the Delivery Bucket Manager processes. Managers are identified by bucket number.
 * The delivery bucket managers persist each notification in the DB while the notification is in-flight.
 *   Once the manager confirms that the notification has been delivered the notification is removed from the DB.
 *
 * This class is intended to represent an immutable object.
 * Please keep it immutable.
 */
public final class Notification
{
  // What and how to deliver the notification to the recipient
  private final Event event; // The event being delivered
  private final DeliveryMethod deliveryMethod; // How and where it is to be delivered

  // Attributes used to track in-flight notifications while delivery attempt is in progress.
  private final Subscription subscription; // Subscription associated with the notification.
  private final int bucketNum; // Bucket/DeliverWorker to which it has been assigned
  private final int recoveryAttemptNum;
  private final Instant lastRecoveryAttempt;
  private final Instant recoveryExpiry;
  private Instant created; // UTC time for when record was created
  private Instant updated; // UTC time for when record was last updated

  public Notification(Event event1, DeliveryMethod deliveryMethod1, Subscription subscription1, int bucketNum1,
                      int recoveryAttemptNum1, Instant lastRecoveryAttempt1, Instant recoveryExpiry1,
                      Instant created1, Instant updated1)
  {
    event = event1;
    deliveryMethod = deliveryMethod1;
    subscription = subscription1;
    bucketNum = bucketNum1;
    recoveryAttemptNum = recoveryAttemptNum1;
    lastRecoveryAttempt = lastRecoveryAttempt1;
    recoveryExpiry = recoveryExpiry1;
    created = created1;
    updated = updated1;
  }

  public Event getEvent() { return event; }
  public DeliveryMethod getDeliveryMethod() { return deliveryMethod; }
  public Subscription getSubscription() { return subscription; }
  public int getBucketNum() { return bucketNum; }
  public int getRecoveryAttemptNum() { return recoveryAttemptNum; }
  public Instant getLastRecoveryAttempt() { return lastRecoveryAttempt; }
  public Instant getRecoveryExpiry() { return recoveryExpiry; }
  public Instant getCreated() { return created; }
  public Instant getUpdated() { return updated; }
}
