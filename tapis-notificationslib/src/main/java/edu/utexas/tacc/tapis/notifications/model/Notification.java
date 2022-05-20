package edu.utexas.tacc.tapis.notifications.model;

import edu.utexas.tacc.tapis.shared.utils.TapisUtils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

/*
 * A Notification is a notice to a subscriber that an event has occurred.
 * Each persisted notification has a UUID that is used as the primary key in the DB.
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
  private final UUID uuid; // Used as primary key in DB
  private final String tenant;
  private final String subscriptionId;
  private final UUID eventUuid; // Needed to find all notifications associated with a specific event.

  // What and how to deliver the notification to the recipient
  private final Event event; // The event being delivered
  private final DeliveryTarget deliveryTarget; // How and where it is to be delivered
  private final Instant created; // UTC time for when record was created

  // Attributes used to track in-flight notifications while delivery attempt is in progress.
  // Mark seqId and bucketNum as transient so Gson will not include it.
  private transient final int subscrSeqId; // Subscription associated with the notification.
  private transient final int bucketNum; // Bucket/DeliverWorker to which it has been assigned

  /**
   * Constructor for jOOQ with input parameter matching order of columns in DB
   * Also useful for testing.
   * If uuid provided is null then a uuid is generated.
   * If created is null then the current system time is used.
   */
  public Notification(UUID uuid1, int subscrSeqId1, String tenant1, String subscrId1, int bucketNum1, UUID eventUuid1,
                      Event event1, DeliveryTarget deliveryTarget1, Instant created1)
  {
    uuid = (uuid1 != null) ? uuid1 : UUID.randomUUID();
    subscrSeqId = subscrSeqId1;
    tenant = tenant1;
    subscriptionId = subscrId1;
    bucketNum = bucketNum1;
    eventUuid = eventUuid1;
    event = event1;
    deliveryTarget = deliveryTarget1;
    created = (created1 != null) ? created1 : TapisUtils.getUTCTimeNow().toInstant(ZoneOffset.UTC);
  }

  public String getTenant() { return tenant; }
  public String getSubscriptionId() { return subscriptionId; }
  public UUID getEventUuid() { return eventUuid; }
  public Event getEvent() { return event; }
  public DeliveryTarget getDeliveryMethod() { return deliveryTarget; }
  public int getSubscrSeqId() { return subscrSeqId; }
  public int getBucketNum() { return bucketNum; }
  public UUID getUuid() { return uuid; }
  public Instant getCreated() { return created; }

  @Override
  public String toString()
  {
    String msg = "UUID: %s%nSubscrSeqId: %d%nTenant: %s%nbucketNum: %d%neventUuid: %s%nEvent %s%nDeliveryMethod: %s%nCreated: %s";
    return msg.formatted(uuid, subscrSeqId, tenant, bucketNum, eventUuid, event, deliveryTarget, created);
  }
}
