package edu.utexas.tacc.tapis.notifications.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.sharedq.QueueManagerNames;
import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.runtime.TapisRuntimeException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.sharedq.QueueManagerParms;
import edu.utexas.tacc.tapis.sharedq.VHostManager;
import edu.utexas.tacc.tapis.sharedq.VHostParms;

import edu.utexas.tacc.tapis.notifications.model.Event;
import edu.utexas.tacc.tapis.notifications.model.Delivery;
import edu.utexas.tacc.tapis.notifications.utils.LibUtils;
import edu.utexas.tacc.tapis.notifications.config.RuntimeParameters;

import static edu.utexas.tacc.tapis.notifications.service.DispatchService.NUM_BUCKETS;

/*
 * Singleton class to provide message broker services:
 *  - initialize connections and channels.
 *  - initialize exchanges and queues.
 *  - publish an event to a queue
 *  - read an event from a queue
 *
 * Notifications service uses a single primary queue for events, so we include all support in this one class.
 */
public final class MessageBroker
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Logging
  private static final Logger log = LoggerFactory.getLogger(MessageBroker.class);

  public static final String VHOST = "NotificationsHost";
  public static final String DEFAULT_BINDING_KEY = "#";
  public static final String EXCHANGE_MAIN = "tapis.notifications.exchange";
  public static final String QUEUE_MAIN = "tapis.notifications.queue";

  /* ********************************************************************** */
  /*                                Enums                                   */
  /* ********************************************************************** */

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */

  // Singleton instance of this class.
  private static MessageBroker instance;

  // RabbitMQ specific parameters. Mostly set from RuntimeParameters.
  private static QueueManagerParms mbParms;

  // Connection for talking to RabbitMQ
  private final ConnectionFactory connectionFactory = new ConnectionFactory();
  private Connection mbConnection;
  private Channel mbChannel;

  /* ********************************************************************** */
  /*                             Constructors                               */
  /* ********************************************************************** */

  /*
   * Private constructor used during init of the singleton
   */
  private MessageBroker() throws TapisRuntimeException
  {
    // Initialize vhost
    initRabbitVHost();

    // Initialize connections, channels, exchanges and queues
    try
    {
      initConnectionAndChannel();
      initExchangesAndQueues();
    }
    catch (Exception e)
    {
      String msg = LibUtils.getMsg("NTFLIB_MSGBRKR_INIT_ERR", mbParms.getService(), mbParms.getInstanceName(),
                                   e.getMessage());
      throw new TapisRuntimeException(msg, e);
    }
  }
  
  /* ********************************************************************** */
  /*                             Public Methods                             */
  /* ********************************************************************** */

  /*
   * Create singleton instance of this class if necessary.
   * This method must be called before getInstance()
   * This called by main Application and Dispatcher
   */
  public synchronized static void init(RuntimeParameters parms) throws TapisRuntimeException
  {
    // Create the singleton instance
    if (instance != null) return;
    // Create and initialize qMgrParameters from RuntimeParameters.
    mbParms = new QueueManagerParms();
    mbParms.setService(TapisConstants.SERVICE_NAME_NOTIFICATIONS);
    mbParms.setVhost(VHOST);
    mbParms.setInstanceName(parms.getInstanceName());
    mbParms.setQueueHost(parms.getQueueHost());
    mbParms.setQueuePort(parms.getQueuePort());
    mbParms.setQueueUser(parms.getQueueUser());
    mbParms.setQueuePassword(parms.getQueuePassword());
    mbParms.setQueueSSLEnabled(parms.isQueueSSLEnabled());
    mbParms.setQueueAutoRecoveryEnabled(parms.isQueueAutoRecoveryEnabled());

    instance = new MessageBroker();
  }
  
  /*
   * Get the singleton instance of this class.
   * The init() method must be called first.
   * Calling this method before init() causes a runtime error.
   */
  public static MessageBroker getInstance() throws TapisRuntimeException
  {
    // The singleton instance must have been created before this method is called.
    if (instance == null)
    {
        String msg = MsgUtils.getMsg("QMGR_UNINITIALIZED_ERROR");
        throw new TapisRuntimeException(msg);
    }
    return instance;
  }

  /**
   * Close channel and connection
   * @param timeoutMs - how long to wait when closing the connection
   */
  public void shutDown(int timeoutMs)
  {
    log.info(LibUtils.getMsg("NTFLIB_MSGBRKR_CONN_CLOSE", timeoutMs));
    // Close channel
    if (mbChannel != null)
    {
      try { mbChannel.close(); }
      catch (Exception e)
      {
        String msg = LibUtils.getMsg("NTFLIB_MSGBRKR_CHAN_CLOSE_ERR", mbChannel.getChannelNumber(), e.getMessage());
        log.error(msg, e);
      }
    }
    // Close connection
    if (mbConnection != null)
    {
      try { mbConnection.close(timeoutMs); }
      catch (Exception e)
      {
        String msg = MsgUtils.getMsg("QMGR_CLOSE_CONN_ERROR", "MessageBroker", e.getMessage());
        log.error(msg, e);
      }
    }
  }

  /**
   * Place an event on the main event queue
   * This is called by NotificationsService.
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param event - Event to queue up
   * @throws IOException - on error
   */
  public void publishEvent(ResourceRequestUser rUser, Event event) throws IOException
  {
    // Convert event to json string
    var jsonMessage = TapisGsonUtils.getGson().toJson(event);
    // Publish the event to the queue.
    getChannel().basicPublish(EXCHANGE_MAIN, QUEUE_MAIN, QueueManagerNames.PERSISTENT_JSON,
                           jsonMessage.getBytes(StandardCharsets.UTF_8));
    if (log.isTraceEnabled())
    {
      log.trace(LibUtils.getMsgAuth("NTFLIB_EVENT_PUB", rUser, event.getSource(), event.getType(), event.getSubject(),
                                    event.getSeriesId(), event.getTime(), event.getUuid()));
    }
  }

  /**
   * Read a message from the main event queue.
   * If autoAck is true then message is removed from the queue.
   * This is called by NotificationsService (for testing)
   *
   * @throws TapisException - on error
   */
  public Event readEvent(boolean autoAck) throws TapisException
  {
    Event retEvent;
    GetResponse resp;
    // Read message from queue.
    try
    {
      resp = getChannel().basicGet(QUEUE_MAIN, autoAck);
    }
    catch (IOException e)
    {
      String msg = LibUtils.getMsg("NTFLIB_EVENT_READ_ERR", e.getMessage());
      throw new TapisException(msg, e);
    }

    // Convert event to json string
    String jsonStr = new String(resp.getBody(), StandardCharsets.UTF_8);
    retEvent = TapisGsonUtils.getGson().fromJson(jsonStr, Event.class);

    return retEvent;
  }

  /**
   * Acknowledge a message so that is removed from the main event queue.
   * This is called by DeliveryBucketManager.
   *
   * @param deliveryTag - deliveryTag provide my message broker
   * @throws IOException - on error
   */
  public void ackMsg(long deliveryTag) throws IOException
  {
     boolean ackMultiple = false; // do NOT ack all messages up to and including the deliveryTag
     mbChannel.basicAck(deliveryTag, ackMultiple);
  }

  /**
   * Create and start the consumer that handles events delivered to the main queue.
   * Return the consumer tag
   * This is called by Dispatcher in order to start receiving events.
   *
   * @param deliveryQueues - in-memory queues used to pass events to bucket manager threads
   * @throws IOException - on error
   * @return consumer tag
   */
  public String startConsumer(List<BlockingQueue<Delivery>> deliveryQueues) throws IOException
  {
    // Create the consumer that handles receiving messages from the queue.
    //   The consumer:
    //     - turns the message into an Event and constructs a Delivery object
    //     - computes the bucket number
    //     - places Delivery on a queue for a bucket manager thread to pick up.
    // The bucket manager thread must do the final message acknowledgement
    Consumer consumer = new DefaultConsumer(mbChannel)
    {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
      {
        // Convert event to json string
        String jsonStr = new String(body, StandardCharsets.UTF_8);
        Event event = TapisGsonUtils.getGson().fromJson(jsonStr, Event.class);
        // Trace receipt of the event
        if (log.isTraceEnabled())
        {
          log.trace(LibUtils.getMsg("NTFLIB_EVENT_RCV", event.getTenantId(), event.getSource(), event.getType(),
                                    event.getSubject(), event.getSeriesId(), event.getTime(), event.getUuid()));
        }

        // Create the Delivery object to be passed to the bucket manager.
        Delivery delivery = new Delivery(event, envelope.getDeliveryTag());

        // Compute the bucket number
        int bucketNum = computeBucketNumber(event);
        // Pass event to bucket manager thread through an in-memory queue
        // NOTE: bucket manager thread uses deliveryTag in order to ack the message
        try
        {
          deliveryQueues.get(bucketNum).put(delivery);
        }
        catch (InterruptedException e)
        {
          String msg = LibUtils.getMsg("NTFLIB_EVENT_PUT_INTRPT", event.getTenantId(), event.getSource(),
                                       event.getType(), event.getSubject(), event.getSeriesId(), event.getUuid());
          log.info(msg);
        }
      }
    };

    // Now start consuming with no auto-ack. Ack should happen once event has been processed and notifications table
    //   has been populated for all subscriptions matching the event.
    boolean autoAck = false;
    String consumerTag;
    consumerTag = mbChannel.basicConsume(QUEUE_MAIN, autoAck, consumer);
    return consumerTag;
  }

  /**
   * Check status
   */
  public Exception checkConnection()
  {
    if (!mbConnection.isOpen())
    {
      return new TapisException(LibUtils.getMsg("NTFLIB_MSGBRKR_NO_CONN"));
    }
    return null;
  }

    /* ********************************************************************** */
  /*                             Private Methods                            */
  /* ********************************************************************** */

  /**
   * Create the exchanges and queues for notification events and bind them together
   */
  private void initExchangesAndQueues() throws IOException
  {
    boolean durable = true;
    boolean autoDelete = false;
    Map<String,Object> exchangeArgs = null;
    boolean exclusive = false;
    // Establish the durable exchange and queue
    getChannel().exchangeDeclare(EXCHANGE_MAIN, BuiltinExchangeType.FANOUT, durable, autoDelete, exchangeArgs);
    getChannel().queueDeclare(QUEUE_MAIN, durable, exclusive, autoDelete, null);
    // Bind to the queue
    getChannel().queueBind(QUEUE_MAIN, EXCHANGE_MAIN, DEFAULT_BINDING_KEY);
  }

  /**
   * Establish the virtual host on the message broker. All interactions with the broker after this will be
   * on the virtual host. If the host already exists and its administrator user has been granted the proper
   * permissions, then this method has no effect.
   *
   * @throws TapisRuntimeException on error
   */
  private void initRabbitVHost() throws TapisRuntimeException
  {
    RuntimeParameters rtParms = RuntimeParameters.getInstance();
    // Collect the runtime message broker information.
    var host  = mbParms.getQueueHost();
    var user  = mbParms.getQueueUser();
    var pass  = mbParms.getQueuePassword();
    var vhost = mbParms.getVhost();
    var adminPort = rtParms.getQueueAdminPort();
    var adminUser = rtParms.getQueueAdminUser();
    var adminPassword = rtParms.getQueueAdminPassword();

    // Create the vhost object and execute the initialization routine.
    var parms = new VHostParms(host, adminPort, adminUser, adminPassword);
    var mgr   = new VHostManager(parms);
    try
    {
      mgr.initVHost(vhost, user, pass);
    }
    catch (Exception e)
    {
      String msg = MsgUtils.getMsg("QMGR_UNINITIALIZED_ERROR");
      throw new TapisRuntimeException(msg, e);
    }
  }

  /**
   * Establish the connection with rabbitmq
   *
   * @throws TapisRuntimeException on error
   */
  private void initConnectionAndChannel() throws IOException, TimeoutException
  {
    connectionFactory.setHost(mbParms.getQueueHost());
    connectionFactory.setPort(mbParms.getQueuePort());
    connectionFactory.setUsername(mbParms.getQueueUser());
    connectionFactory.setPassword(mbParms.getQueuePassword());
    connectionFactory.setAutomaticRecoveryEnabled(mbParms.isQueueAutoRecoveryEnabled());
    connectionFactory.setVirtualHost(mbParms.getVhost());
    mbConnection = connectionFactory.newConnection();
    mbChannel = mbConnection.createChannel();
  }

  /*
   * Get the channel
   * Channels can close due to exceptions, re-create as needed.
   * NOTE: although it is recommended that isOpen not be used in production code because there can be race conditions,
   *   in our case we only have one thread that deals with rabbitmq (the DispatchApplication), so we will not have
   *   multiple threads accessing the channel.
   */
  private Channel getChannel() throws IOException
  {
    if (mbChannel.isOpen()) return mbChannel;
    else return mbConnection.createChannel();
  }

  /*
   * Compute a bucket number from 0 to NUM_BUCKETS-1
   *   based on hash of event source, subject and seriesId
   * NOTE that we clear the top 4 bits of the hash so we always have a positive integer
   */
  private static int computeBucketNumber(Event event)
  {
    Object[] hashObjects = {event.getSource(), event.getSubject(), event.getSeriesId()};
    int hash = (Arrays.hashCode(hashObjects) & 0xfffffff);
    return hash % NUM_BUCKETS;
  }
}