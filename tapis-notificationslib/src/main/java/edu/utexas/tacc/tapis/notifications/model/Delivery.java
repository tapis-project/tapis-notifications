package edu.utexas.tacc.tapis.notifications.model;

/*
 * A Delivery represents an event that is being processed in order to determine matching subscribers and
 *   create one or more notifications for each subscriber.
 * It contains the Event being processed and the message broker tag (rabbitmq deliveryTag).
 * The deliveryTag is used to acknowledge the message once all notifications have been generated and persisted.
 *
 * This class is intended to represent an immutable object.
 * Please keep it immutable.
 *
 */
public final class Delivery
{
  private final Event event;
  private final long deliveryTag;

  public Delivery(Event event1, long deliveryTag1)
  {
    event = event1;
    deliveryTag = deliveryTag1;
  }

  public Event getEvent() { return event; }

  public long getDeliveryTag() { return deliveryTag; }
}
