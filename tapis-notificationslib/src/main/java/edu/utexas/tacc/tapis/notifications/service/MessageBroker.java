package edu.utexas.tacc.tapis.notifications.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.GetResponse;
import edu.utexas.tacc.tapis.sharedq.DeliveryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import edu.utexas.tacc.tapis.notifications.utils.LibUtils;
import edu.utexas.tacc.tapis.notifications.config.RuntimeParameters;

/*
 * Singleton class to provide message broker services:
 *  - initialize create connections and channels.
 *  - initialize exchanges and queues.
 *  - publish an event to a queue
 *  - read an event from a queue
 *
 * Notifications uses a single primary queue for events, so we include all support in this one service.
 * TODO/TBD: What about ALT, DEADLETTER and RECOVERY queues?
 */
public final class MessageBroker // extends AbstractQueueManager
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Tracing.
  private static final Logger log = LoggerFactory.getLogger(MessageBroker.class);

  public static final String VHOST = "NotificationsHost";
  public static final String DEFAULT_BINDING_KEY = "#";
  public static final String EXCHANGE_MAIN = "tapis.notifications.exchange";
  public static final String QUEUE_MAIN = "tapis.notifications.queue";

  /* ********************************************************************** */
  /*                                Enums                                   */
  /* ********************************************************************** */
// TODO  public enum ExchangeName {MAIN, RECOVERY, ALT, DEAD}

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */
  // Singleton instance of this class.
  private static MessageBroker instance;

  // RabbitMQ specific parameters. Set from RuntimeParameters.
  private static QueueManagerParms qMgrParms;

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
      String msg = LibUtils.getMsg("NTFLIB_MSGBRKR_INIT_ERR", qMgrParms.getService(), qMgrParms.getInstanceName(),
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
   */
  public static void init(RuntimeParameters parms) throws TapisRuntimeException
  {
    // Create the singleton instance without setting up a synchronized block in the common case.
    if (instance == null)
    {
      synchronized (MessageBroker.class)
      {
        // Construct QueueManagerParms for the super-class constructor
        qMgrParms = new QueueManagerParms();
        qMgrParms.setService(TapisConstants.SERVICE_NAME_NOTIFICATIONS);
        qMgrParms.setVhost(VHOST);
        qMgrParms.setInstanceName(parms.getInstanceName());
        qMgrParms.setQueueHost(parms.getQueueHost());
        qMgrParms.setQueuePort(parms.getQueuePort());
        qMgrParms.setQueueUser(parms.getQueueUser());
        qMgrParms.setQueuePassword(parms.getQueuePassword());
        qMgrParms.setQueueSSLEnabled(parms.isQueueSSLEnabled());
        qMgrParms.setQueueAutoRecoveryEnabled(parms.isQueueAutoRecoveryEnabled());

        if (instance == null) instance = new MessageBroker();
      }
    }
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
    // Close channel
    if (mbChannel != null && mbChannel.isOpen())
    {
      try { mbChannel.close(); }
      catch (Exception e)
      {
//        String msg = LibUtils.getMsgAuth("NTFLIB_MSGBRKR_CHAN_CLOSE_ERR", rUser, channel.getChannelNumber(),
//                e.getMessage());
        String msg = "Error closing channel: " + e.getMessage();
        log.error(msg, e);
      }
    }
    // Close connection
    if (mbConnection != null && mbConnection.isOpen())
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
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param event - Event to queue up
   * @throws IOException - on error
   */
  public void publishEvent(ResourceRequestUser rUser, Event event) throws IOException
  {
    // Convert event to json string
    var jsonMessage = TapisGsonUtils.getGson().toJson(event);
//    try
//    {
    // Publish the event to the queue.
    getChannel().basicPublish(EXCHANGE_MAIN, QUEUE_MAIN, QueueManagerNames.PERSISTENT_JSON,
                           jsonMessage.getBytes(StandardCharsets.UTF_8));
    if (log.isTraceEnabled())
    {
      log.trace(LibUtils.getMsgAuth("NTFLIB_EVENT_PUB", rUser, event.getSource(), event.getType(), event.getSubject()));
    }
//  }
//    catch (IOException e)
//    {
//      String msg = LibUtils.getMsgAuth("NTFLIB_EVENT_PUB_ERR", rUser, event.getSource(), event.getType(),
//                                       event.getSubject(), e.getMessage());
//      throw new TapisException(msg, e);
//    }
  }

  /**
   * Read a message from the primary event queue.
   * If autoAck is true then message is removed from the queue.
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @throws TapisException - on error
   */
  public Event readEvent(ResourceRequestUser rUser, boolean autoAck) throws TapisException
  {
    Event retEvent = null;
    GetResponse resp = null;
    // Read message from queue. The deliverCallback method will process it.
    try
    {
      resp = getChannel().basicGet(QUEUE_MAIN, autoAck);
//      getChannel().basicConsume(QUEUE_MAIN, autoAck, deliverCallback, consumerTag -> {});
    }
    catch (IOException e)
    {
      String msg = LibUtils.getMsgAuth("NTFLIB_EVENT_CON_ERR", rUser, e.getMessage());
      throw new TapisException(msg, e);
    }

    // Convert event to json string
    String jsonStr = new String(resp.getBody(), StandardCharsets.UTF_8);
    retEvent = TapisGsonUtils.getGson().fromJson(jsonStr, Event.class);

    return retEvent;
  }


  // ************************************************************************
  // **************************  Private Methods  ***************************
  // ************************************************************************

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
    var host  = qMgrParms.getQueueHost();
    var user  = qMgrParms.getQueueUser();
    var pass  = qMgrParms.getQueuePassword();
    var vhost = qMgrParms.getVhost();
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
    connectionFactory.setHost(qMgrParms.getQueueHost());
    connectionFactory.setPort(qMgrParms.getQueuePort());
    connectionFactory.setUsername(qMgrParms.getQueueUser());
    connectionFactory.setPassword(qMgrParms.getQueuePassword());
    connectionFactory.setAutomaticRecoveryEnabled(qMgrParms.isQueueAutoRecoveryEnabled());
    connectionFactory.setVirtualHost(qMgrParms.getVhost());
    mbConnection = connectionFactory.newConnection();
    mbChannel = mbConnection.createChannel();
  }


  /*
   * Get the channel
   * Channels can close due to exceptions, re-create as needed.
   * TODO/TBD: Channel instances should not be shared between threads.
   *  re-visit this when spawning threads to handle processing of events.
   *  Currently we have a singleton MessageBroker with a single connection and channel.
   *  Should be able to share connection.
   */
  private Channel getChannel() throws IOException
  {
    if (mbChannel.isOpen()) return mbChannel;
    else return mbConnection.createChannel();
  }

}