package edu.utexas.tacc.tapis.notifications.model;

import java.time.Instant;
import java.util.UUID;

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
  private final int seqId;     // Unique database sequence number
  private final String tenant;
  private final UUID eventUuid;

  // What and how to deliver the notification to the recipient
  private final Event event; // The event being delivered
  private final DeliveryMethod deliveryMethod; // How and where it is to be delivered

  // Attributes used to track in-flight notifications while delivery attempt is in progress.
  private final int subscrSeqId; // Subscription associated with the notification.
  private final int bucketNum; // Bucket/DeliverWorker to which it has been assigned
  private final Instant created; // UTC time for when record was created

  /**
   * Constructor for jOOQ with input parameter matching order of columns in DB
   * Also useful for testing
   */
  public Notification(int seqId1, int subscrSeqId1, String tenant1, int bucketNum1, UUID eventUuid1, Event event1,
                      DeliveryMethod deliveryMethod1, Instant created1)
  {
    seqId = seqId1;
    subscrSeqId = subscrSeqId1;
    tenant = tenant1;
    bucketNum = bucketNum1;
    eventUuid = eventUuid1;
    event = event1;
    deliveryMethod = deliveryMethod1;
    created = created1;
  }

  public Event getEvent() { return event; }
  public DeliveryMethod getDeliveryMethod() { return deliveryMethod; }
  public int getSubscrSeqId() { return subscrSeqId; }
  public int getBucketNum() { return bucketNum; }
  public Instant getCreated() { return created; }

  @Override
  public String toString()
  {
    String msg = "SeqId: %d%nSubscrSeqId: %d%nTenant: %s%nbucketNum: %d%neventUuid: %s%nEvent %s%nDeliveryMethod: %s%nCreated: %s";
    return msg.formatted(seqId, subscrSeqId, tenant, bucketNum, eventUuid, event, deliveryMethod, created);
  }
}
