package edu.utexas.tacc.tapis.notifications.queue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import edu.utexas.tacc.tapis.notifications.model.Event;
import edu.utexas.tacc.tapis.notifications.utils.LibUtils;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.sharedq.QueueManagerNames;
import edu.utexas.tacc.tapis.sharedq.exceptions.TapisQueueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import edu.utexas.tacc.tapis.jobs.exceptions.JobException;
//import edu.utexas.tacc.tapis.jobs.exceptions.JobQueueException;
//import edu.utexas.tacc.tapis.jobs.model.Job;
//import edu.utexas.tacc.tapis.jobs.queue.messages.JobSubmitMsg;
//import edu.utexas.tacc.tapis.jobs.queue.messages.cmd.CmdMsg;
//import edu.utexas.tacc.tapis.jobs.queue.messages.recover.RecoverMsg;
import edu.utexas.tacc.tapis.notifications.config.RuntimeParameters;
import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.runtime.TapisRuntimeException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.sharedq.AbstractQueueManager;
import edu.utexas.tacc.tapis.sharedq.QueueManagerParms;
import edu.utexas.tacc.tapis.sharedq.VHostManager;
import edu.utexas.tacc.tapis.sharedq.VHostParms;

/*
 * Singleton class to manage the message broker
 */
public final class MessageBrokerManager extends AbstractQueueManager
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Tracing.
  private static final Logger log = LoggerFactory.getLogger(MessageBrokerManager.class);

  public static final String VHOST = "NotificationsHost";
  public static final String DEFAULT_BINDING_KEY = "#";
  public static final String EXCHANGE_NAME = "tapis.notifications.submit.exchange";
  public static final String QUEUE_NAME = "tapis.notifications.submit.defaultQueue";

  /* ********************************************************************** */
  /*                                Enums                                   */
  /* ********************************************************************** */
//  public enum ExchangeUse {ALT, DEAD, OTHER} // TODO needed?

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */
  // Singleton instance of this class.
  private static MessageBrokerManager _instance;

  // Parameters for the super-class constructor.
  private static QueueManagerParms qMgrParms;

  /* ********************************************************************** */
  /*                             Constructors                               */
  /* ********************************************************************** */

  /*
   * Private constructor used during init of the singleton
   */
  private MessageBrokerManager() throws TapisRuntimeException
  {
    super(qMgrParms);

    // Initialize vhost.
    initRabbitVHost();

    // Create the queues needed by most if not all applications.
    //  do we need to create these? or just the single exchange and queue that will accept notif events?
    try
    {
      createStandardQueues();
    }
    catch (Exception e)
    {
      String msg = LibUtils.getMsg("NTFLIB_MSGBRKR_INIT_ERR", qMgrParms.getService(), qMgrParms.getInstanceName());
      throw new TapisRuntimeException(msg, e);
    }

    // Create the exchange and queue for notification events
    try
    {
      createNotifEventExchangeAndQueue();
    }
    catch (Exception e)
    {
      String msg = LibUtils.getMsg("NTFLIB_MSGBRKR_INIT_ERR", qMgrParms.getService(), qMgrParms.getInstanceName());
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
    // Create the singleton instance if it's null without
    // setting up a synchronized block in the common case.
    if (_instance == null)
    {
      synchronized (MessageBrokerManager.class)
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

        if (_instance == null) _instance = new MessageBrokerManager();
      }
    }
  }
  
  /**
   * Get the singleton instance of this class.
   * The init() method must be called first.
   * Calling this method before init() causes a runtime error.
   * 
   * @return the existing singleton
   */
  public static MessageBrokerManager getInstance() throws TapisRuntimeException
  {
    // The singleton instance must have been created before this method is called.
    if (_instance == null)
    {
        String msg = MsgUtils.getMsg("QMGR_UNINITIALIZED_ERROR");
        throw new TapisRuntimeException(msg);
    }
    return _instance;
  }

  /**
   * Place an event on the notifications event queue
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param event - Event to queue up
   * @throws TapisException - on error
   */
  public void queueEvent(ResourceRequestUser rUser, Event event) throws TapisException
  {
//  /** Write a json message to the named tenant queue.  The queue name is used
//   * as the routing key on the direct exchange.
//   *
//   * @param queueName the target queue name
//   * @param message a json string
//   */
//  public void postSubmitQueue(String queueName, String message) throws JobException
//  {

    // Convert event to json string
    var jsonMessage = TapisGsonUtils.getGson().toJson(event);

    // Flag indicating if channel should be aborted.
    // TODO/TBD: Is this needed?
    boolean abortChannel = false;

    // Create a temporary channel.
    Channel channel = getNewOutChannel();

    // Publish the message to the queue.
    try
    {
      // Publish the event to the queue
      channel.basicPublish(EXCHANGE_NAME, QUEUE_NAME, QueueManagerNames.PERSISTENT_JSON,
                           jsonMessage.getBytes(StandardCharsets.UTF_8));
      if (log.isTraceEnabled())
      {
        log.trace(LibUtils.getMsgAuth("NTFLIB_EVENT_PUB", rUser, event.getSource(), event.getType(), event.getSubject()));
      }
    }
    catch (IOException e)
    {
      //TODO/TBD We were able to create the channel but not able to publish. Set flag so channel is aborted.
      abortChannel = true;
      String msg = LibUtils.getMsgAuth("NTFLIB_EVENT_PUB_ERR", rUser, event.getSource(), event.getType(),
                                       event.getSubject(), e.getMessage());
      throw new TapisException(msg, e);
    }
    finally
    {
      // Channel clean up
      if (channel != null)
      {
        try
        {
          // TODO/TBD: Is this needed? Cannot find any doc that discusses this.
          if (abortChannel) channel.abort(); else channel.close();
        }
        catch (Exception e)
        {
          String msg = LibUtils.getMsgAuth("NTFLIB_MSGBRKR_CHAN_CLOSE_ERR", rUser, channel.getChannelNumber(),
                                           e.getMessage());
          log.error(msg, e);
        }
      }
    }
  }

//  /* ---------------------------------------------------------------------- */
//  /* getDefaultQueue:                                                       */
//  /* ---------------------------------------------------------------------- */
//  public static String getDefaultQueue()
//  {
//    return JobQueueManagerNames.getDefaultQueue();
//  }
//
//  /* ---------------------------------------------------------------------- */
//  /* JobQueueManagerParms:                                                  */
//  /* ---------------------------------------------------------------------- */
//  public static JobQueueManagerParms initParmsFromRuntime()
//  {
//      // Set up broker initialization.
//      var rt = RuntimeParameters.getInstance();
//      var qmParms = new JobQueueManagerParms();
//      qmParms.setQueueHost(rt.getQueueHost());
//      qmParms.setQueuePort(rt.getQueuePort());
//      qmParms.setQueueUser(rt.getQueueUser());
//      qmParms.setQueuePassword(rt.getQueuePassword());
//      qmParms.setAdminUser(rt.getQueueAdminUser());
//      qmParms.setAdminPassword(rt.getQueueAdminPassword());
//      qmParms.setAdminPort(rt.getQueueAdminPort());
//      qmParms.setQueueSSLEnabled(rt.isQueueSSLEnabled());
//      qmParms.setQueueAutoRecoveryEnabled(rt.isQueueAutoRecoveryEnabled());
//      qmParms.setVhost(JobQueueManager.JOBS_VHOST);
//      qmParms.setService(TapisConstants.SERVICE_NAME_JOBS);
//      qmParms.setInstanceName(rt.getInstanceName());
//      return qmParms;
//  }
//
//  /* ---------------------------------------------------------------------------- */
//  /* queueJob:                                                                    */
//  /* ---------------------------------------------------------------------------- */
//  public void queueJob(Job job) throws JobException
//  {
//      // Create the message.
//      var message = new JobSubmitMsg();
//      message.setCreated(job.getCreated().toString());
//      message.setUuid(job.getUuid());
//      var jsonMessage = TapisGsonUtils.getGson().toJson(message);
//      postSubmitQueue(job.getTapisQueue(), jsonMessage);
//  }
//
//  /* ---------------------------------------------------------------------- */
//  /* doRefreshQueueInfo:                                                    */
//  /* ---------------------------------------------------------------------- */
//  /** Force all schedulers to refresh their queue caches.  This action allows
//   * any changes to queue definitions in the job_queues table to be loaded into
//   * memory.  For example, if the priority of a queue has changed in the
//   * database, its new priority will be respected after this method executes.
//   *
//   * How This Works
//   * --------------
//   * Users can execute code in JobQueueDao to change the persistent information
//   * about queues, such as the priority of a queue.  These changes, however,
//   * are not reflected in the running system until this method is executed and
//   * the cached _tenantQueues map is replaced.
//   *
//   * Being able to switch out the old _tenantQueues map for a new one in a
//   * multithreaded environment depends on guarantees implemented in the JVM.
//   * Specifically, the Java Language Specification requires that reference assignments
//   * be atomically performed.  That is, when updating a reference to an object, all
//   * threads see either the old address or the new address, but never a mixture of
//   * the two.  Here's the quote from Java 7's Java Language Specification, section 17.7:
//   *
//   *    Writes to and reads of references are always atomic, regardless
//   *    of whether they are implemented as 32-bit or 64-bit values.
//   *
//   * Our usage of the _tenantQueues mapping abides by these rules:
//   *
//   *  1) The field is only written on initialization and in this method.
//   *  2) Once created, the mapping is never modified.  This method updates
//   *     the mapping by overwriting the field with a completely new reference.
//   *  3) All reads of the field are single, independent accesses that do
//   *     not intermix data with other reads of the field.
//   *
//   * These three access patterns, taken together with Java's atomicity guarantee,
//   * means that the _tenantQueues reference can be overwritten at any time
//   * without synchronization between reader and writer threads.
//   *
//   * @return true to acknowledge the message, false to reject it
//   */
//  public boolean doRefreshQueueInfo(//JobCommand jobCommand,
//                                    Envelope envelope,
//                                    BasicProperties properties,
//                                    byte[] body)
//  {
//    // TODO: implement doRefreshQueueInfo()
//    return false;
//  }
//
//  /* ---------------------------------------------------------------------- */
//  /* postSubmitQueue:                                                       */
//  /* ---------------------------------------------------------------------- */
//  /** Write a json message to the named tenant queue.  The queue name is used
//   * as the routing key on the direct exchange.
//   *
//   * @param queueName the target queue name
//   * @param message a json string
//   */
//  public void postSubmitQueue(String queueName, String message)
//    throws JobException
//  {
//    // Create a temporary channel.
//    Channel channel = null;
//    boolean abortChannel = false;
//    try {
//      // Create a temporary channel.
//      try {channel = getNewOutChannel();}
//        catch (Exception e) {
//          String msg = LibUtils.getMsg("JOBS_QMGR_OUT_CHANNEL_ERROR");
//          throw new JobException(msg, e);
//        }
//
//      // Get the tenant exchange name.
//      String exchangeName = JobQueueManagerNames.getSubmitExchangeName();
//
//      // Publish the message to the queue.
//      try {
//        // Write the job to the selected tenant worker queue.
//        channel.basicPublish(exchangeName, queueName, JobQueueManagerNames.PERSISTENT_JSON,
//                             message.getBytes("UTF-8"));
//
//        // Tracing.
//        if (log.isDebugEnabled()) {
//            String msg = LibUtils.getMsg("JOBS_QMGR_POST", exchangeName, queueName);
//            log.debug(msg);
//        }
//      }
//      catch (Exception e) {
//          String msg = LibUtils.getMsg("JOBS_QMGR_PUBLISH_ERROR", exchangeName,
//                                       getOutConnectionName(), channel.getChannelNumber(),
//                                       e.getMessage());
//          throw new JobQueueException(msg, e);
//      }
//    }
//    catch (Exception e) {
//      // Affect the way we close the channel and then rethrow exception.
//      abortChannel = true;
//      throw new JobException(e.getMessage(), e);
//    }
//    finally {
//      // Channel clean up
//      if (channel != null) {
//        try {
//          // Close the channel one way or the other.
//          if (abortChannel) channel.abort();
//            else channel.close();
//        }
//          catch (Exception e) {
//            String msg = LibUtils.getMsg("JOBS_QMGR_CHANNEL_CLOSE_ERROR", channel.getChannelNumber(),
//                                         e.getMessage());
//            log.error(msg, e);
//          }
//      }
//    }
//  }
//
//  /* ---------------------------------------------------------------------- */
//  /* postTopic:                                                             */
//  /* ---------------------------------------------------------------------- */
//  /** Write a json message to the named topic.  The routing key
//   * determines which workers receive the message.
//   *
//   * @param exchangeName the target exchange name
//   * @param message a json string
//   */
//  public void postTopic(String exchangeName, String message, String routingKey)
//    throws JobException
//  {
//    // Create a temporary channel.
//    Channel channel = null;
//    boolean abortChannel = false;
//    try {
//      // Create a temporary channel.
//      channel = getNewOutChannel();
//
//      // Publish the message to the queue.
//      try {
//        // Write the job to the selected tenant worker queue.
//        channel.basicPublish(exchangeName, routingKey, JobQueueManagerNames.PERSISTENT_JSON,
//                             message.getBytes("UTF-8"));
//
//        // Tracing.
//        if (log.isDebugEnabled()) {
//            String msg = LibUtils.getMsg("JOBS_QMGR_POST", exchangeName, routingKey);
//            log.debug(msg);
//        }
//      }
//      catch (Exception e) {
//          String msg = LibUtils.getMsg("JOBS_QMGR_PUBLISH_ERROR", exchangeName,
//                                       getOutConnectionName(), channel.getChannelNumber(),
//                                       e.getMessage());
//          throw new JobQueueException(msg, e);
//      }
//    }
//    catch (Exception e) {
//      // Affect the way we close the channel and then rethrow exception.
//      abortChannel = true;
//      throw new JobException(e.getMessage(), e);
//    }
//    finally {
//      // Channel clean up
//      if (channel != null) {
//        try {
//          // Close the channel one way or the other.
//          if (abortChannel) channel.abort();
//            else channel.close();
//        }
//          catch (Exception e) {
//            String msg = LibUtils.getMsg("JOBS_QMGR_CHANNEL_CLOSE_ERROR", channel.getChannelNumber(),
//                                         e.getMessage());
//            log.error(msg, e);
//          }
//      }
//    }
//  }
//
//  /* ---------------------------------------------------------------------- */
//  /* postRecoveryQueue:                                                     */
//  /* ---------------------------------------------------------------------- */
//  /** Write a json message to the named tenant queue.  The queue name is used
//   * as the routing key on the direct exchange.
//   *
//   * @param queueName the target queue name
//   * @param message a json string
//   */
//  public void postRecoveryQueue(String message)
//    throws JobException
//  {
//    // Get the exchange and queuenames.
//    String queueName    = JobQueueManagerNames.getRecoveryQueueName();
//    String exchangeName = JobQueueManagerNames.getRecoveryExchangeName();
//
//    // Create a temporary channel.
//    Channel channel = null;
//    boolean abortChannel = false;
//    try {
//      // Create a temporary channel.
//      try {channel = getNewOutChannel();}
//        catch (Exception e) {
//          String msg = LibUtils.getMsg("JOBS_QMGR_OUT_CHANNEL_ERROR");
//          throw new JobException(msg, e);
//        }
//
//      // Publish the message to the queue.
//      try {
//        // Write the job to the tenant recovery queue.
//        channel.basicPublish(exchangeName, DEFAULT_BINDING_KEY, JobQueueManagerNames.PERSISTENT_JSON,
//                             message.getBytes("UTF-8"));
//
//        // Tracing.
//        if (log.isDebugEnabled()) {
//            String msg = LibUtils.getMsg("JOBS_QMGR_POST", exchangeName, queueName);
//            log.debug(msg);
//        }
//      }
//      catch (Exception e) {
//          String msg = LibUtils.getMsg("JOBS_QMGR_PUBLISH_ERROR", exchangeName,
//                                       getOutConnectionName(), channel.getChannelNumber(),
//                                       e.getMessage());
//          throw new JobQueueException(msg, e);
//      }
//    }
//    catch (Exception e) {
//      // Affect the way we close the channel and then rethrow exception.
//      abortChannel = true;
//      throw new JobException(e.getMessage(), e);
//    }
//    finally {
//      // Channel clean up
//      if (channel != null) {
//        try {
//          // Close the channel one way or the other.
//          if (abortChannel) channel.abort();
//            else channel.close();
//        }
//          catch (Exception e) {
//            String msg = LibUtils.getMsg("JOBS_QMGR_CHANNEL_CLOSE_ERROR", channel.getChannelNumber(),
//                                         e.getMessage());
//            log.error(msg, e);
//          }
//      }
//    }
//  }
//
//  /* ---------------------------------------------------------------------- */
//  /* postCmdToAllWorkers:                                                   */
//  /* ---------------------------------------------------------------------- */
//  /** Post a command that is read by all workers.
//   *
//   * @param cmdMsg the command
//   * @throws JobException on error
//   */
//  public void postCmdToAllWorkers(CmdMsg cmdMsg)
//    throws JobException
//  {
//      // Convert command object to a json string.
//      String json = TapisGsonUtils.getGson().toJson(cmdMsg);
//
//      // Get the tenant id, command topic name and all worker routing key.
//      String exchangeName  = JobQueueManagerNames.getCmdExchangeName();
//      String routingKey = JobQueueManagerNames.getCmdAllWorkerRoutingKey();
//
//      // Call the actual post routine.
//      postTopic(exchangeName, json, routingKey);
//  }
//
//  /* ---------------------------------------------------------------------- */
//  /* postCmdToWorker:                                                       */
//  /* ---------------------------------------------------------------------- */
//  /** Post a command that is read by a specific worker.
//   *
//   * @param cmdMsg the command
//   * @throws JobException on error
//   */
//  public void postCmdToWorker(CmdMsg cmdMsg, String workerUuid)
//    throws JobException
//  {
//      // Convert command object to a json string.
//      String json = TapisGsonUtils.getGson().toJson(cmdMsg);
//
//      // Get the tenant id, command topic name and specific worker routing key.
//      String exchangeName  = JobQueueManagerNames.getCmdExchangeName();
//      String routingKey = JobQueueManagerNames.getCmdSpecificWorkerRoutingKey(workerUuid);
//
//      // Call the actual post routine.
//      postTopic(exchangeName, json, routingKey);
//  }
//
//  /* ---------------------------------------------------------------------- */
//  /* postCmdToJob:                                                          */
//  /* ---------------------------------------------------------------------- */
//  /** Post a command that is read by the worker currently servicing a specific
//   * job, if such a worker exists.
//   *
//   * @param cmdMsg the command
//   * @throws JobException on error
//   */
//  public void postCmdToJob(CmdMsg cmdMsg, String jobUuid)
//    throws JobException
//  {
//      // Convert command object to a json string.
//      String json = TapisGsonUtils.getGson().toJson(cmdMsg);
//
//      // Get the tenant id, command topic name and specific job routing key.
//      String exchangeName  = JobQueueManagerNames.getCmdExchangeName();
//      String routingKey = JobQueueManagerNames.getCmdSpecificJobRoutingKey(jobUuid);
//
//      // Call the actual post routine.
//      postTopic(exchangeName, json, routingKey);
//  }
//
//  /* ---------------------------------------------------------------------- */
//  /* postRecoveryQueue:                                                     */
//  /* ---------------------------------------------------------------------- */
//  /** Post a recovery command to the recovery queue.
//   *
//   * @param recoverMsg the recovery command
//   * @throws JobException on error
//   */
//  public void postRecoveryQueue(RecoverMsg recoverMsg)
//    throws JobException
//  {
//      // Convert command object to a json string.
//      String json = TapisGsonUtils.getGson().toJson(recoverMsg);
//
//      // Call the actual post routine.
//      postRecoveryQueue(json);
//  }
//
//  /* ---------------------------------------------------------------------- */
//  /* unbindWorkerSpecificCmdTopic:                                          */
//  /* ---------------------------------------------------------------------- */
//  /** Remove a specific worker's binding from its tenant's command exchange.
//   * This method makes a best effort attempt to unbind the topic from the
//   * exchange and logs an error in case of failure.  In all cases, the newly
//   * created channel is closed.
//   *
//   * @param tenantId the worker's tenant
//   * @param wkrName the worker's name assigned at startup
//   * @param workerUuid the worker's uuid
//   * @param channel a channel on which to issue the command
//   */
//  public void unbindWorkerSpecificCmdTopic(String wkrName, String workerUuid)
//  {
//      // Create the names needed to unbind a queue from an exchange.
//      String queue      = JobQueueManagerNames.getCmdTopicName(wkrName);
//      String exchange   = JobQueueManagerNames.getCmdExchangeName();
//      String bindingKey = JobQueueManagerNames.getCmdSpecificWorkerBindingKey(workerUuid);
//
//      // Get a new channel.  This method always closes the channel.
//      Channel channel = null;
//      try {channel = getNewOutChannel();}
//      catch (Exception e) {
//          String msg = LibUtils.getMsg("JOBS_QMGR_Q_UNBIND_ERROR", "topic",
//                                       queue, bindingKey, exchange, e.getMessage());
//          log.error(msg, e);
//          return;
//      }
//
//      // Unbind.
//      try {channel.queueUnbind(queue, exchange, bindingKey);}
//      catch (Exception e) {
//          String msg = LibUtils.getMsg("JOBS_QMGR_Q_UNBIND_ERROR", "topic",
//                                       queue, bindingKey, exchange, e.getMessage());
//          log.error(msg, e);
//      }
//
//      // Close the just created channel.
//      try {channel.close();}
//      catch (Exception e) {
//          String msg = LibUtils.getMsg("JOBS_QMGR_CHANNEL_CLOSE_ERROR",
//                                       channel.getChannelNumber(), e.getMessage());
//          log.warn(msg, e);
//      }
//  }
//
//  /* ---------------------------------------------------------------------- */
//  /* getExchangeArgs:                                                       */
//  /* ---------------------------------------------------------------------- */
//  /** Get the configuration arguments for the standard exchanges created for
//   * tenants.  Using these arguments both senders and receivers can create the
//   * exchange since it will be created in exactly the same way in both cases.
//   *
//   * @param tenantId the tenant who is creating an exchange
//   * @return the exchange configuration arguments
//   */
//  public Map<String,Object> getExchangeArgs(ExchangeUse etype)
//  {
//      // Dead letter exchanges don't get configured with any other exchanges.
//      if (etype == ExchangeUse.DEAD) return null;
//
//      // Create the argument mapping.
//      var args = getExchangeArgs();
//      if (etype == ExchangeUse.ALT) args.remove("alternate-exchange");
//      return args;
//  }
//
//  /* ---------------------------------------------------------------------- */
//  /* getExchangeArgs:                                                       */
//  /* ---------------------------------------------------------------------- */
//  /** Get the configuration arguments for the standard exchanges created for
//   * tenants.  Using these arguments both senders and receivers can create the
//   * exchange since it will be created in exactly the same way in both cases.
//   *
//   * @param tenantId the tenant who is creating an exchange
//   * @return the exchange configuration arguments
//   */
//  public Map<String,Object> getExchangeArgs()
//  {
//      // Create the argument mapping.
//      HashMap<String,Object> args = new HashMap<>();
//
//      args.put("x-dead-letter-exchange", JobQueueManagerNames.getDeadLetterExchangeName());
//      args.put("alternate-exchange", JobQueueManagerNames.getAltExchangeName());
//      return args;
//  }
//
  /* ********************************************************************** */
  /*                             Private Methods                            */
  /* ********************************************************************** */

//  /** Create all the exchanges and queues that do not depend on tenants or
//   * individual jobs.  These artifacts are used globally.
//   *
//   * @throws TapisQueueException
//   */
//  @SuppressWarnings("unchecked")
//  private void createStandardJobQueues() throws TapisQueueException
//  {
//      String service = _parms.getService();
//      Channel channel = null;
//      try {
//          // Create a temporary channel.
//          channel = getNewInChannel();
//
//          // The alternate and dead letter exchanges and queues were created
//          // in our superclass constructor, so we can reference them here.
//          HashMap<String,Object> exchangeArgs = new HashMap<>();
//          exchangeArgs.put("x-dead-letter-exchange", JobQueueManagerNames.getDeadLetterExchangeName());
//          exchangeArgs.put("alternate-exchange", JobQueueManagerNames.getAltExchangeName());
//
//          // Create the command exchange but not any queues--those are
//          // dynamically created by workers.
//          createExchangeAndQueue(channel, service,
//                                 JobQueueManagerNames.getCmdExchangeName(), BuiltinExchangeType.TOPIC,
//                                 null, null,
//                                 (HashMap<String, Object>) exchangeArgs.clone());
//
//          // Create the recovery exchange and queue and bind them together.
//          createExchangeAndQueue(channel, service,
//                                 JobQueueManagerNames.getRecoveryExchangeName(), BuiltinExchangeType.FANOUT,
//                                 JobQueueManagerNames.getRecoveryQueueName(), DEFAULT_BINDING_KEY,
//                                 (HashMap<String, Object>) exchangeArgs.clone());
//      }
//      finally {
//          // Close the channel if it exists and hasn't already been aborted.
//          if (channel != null)
//            try {channel.close();}
//                catch (Exception e1) {
//                    String msg = LibUtils.getMsg("JOBS_QMGR_CHANNEL_CLOSE_ERROR",
//                                                 channel.getChannelNumber(), e1.getMessage());
//                    log.warn(msg, e1);
//                }
//      }
//  }
//
//  /* ---------------------------------------------------------------------- */
//  /* createSubmitQueues:                                                    */
//  /* ---------------------------------------------------------------------- */
//  private void createSubmitQueues() throws TapisQueueException
//  {
//      // Get the queues.
//      var queues = SubmitQueues.getQueues();
//
//      // Create each queue and its exchange.
//      Channel channel = null;
//      try {
//          // Create a temporary channel.
//          channel = getNewInChannel();
//
//          // The alternate and dead letter exchanges and queues were created
//          // in our superclass constructor, so we can reference them here.
//          HashMap<String,Object> exchangeArgs = new HashMap<>();
//          exchangeArgs.put("x-dead-letter-exchange", JobQueueManagerNames.getDeadLetterExchangeName());
//          exchangeArgs.put("alternate-exchange", JobQueueManagerNames.getAltExchangeName());
//
//          // Exchange name is he same for all submit queues.
//          final boolean durable = true;
//          final boolean autodelete = false;
//          final String exchangeName = JobQueueManagerNames.getSubmitExchangeName();
//
//          // Each queue gets its own exchange.
//          boolean exchangeCreated = false;
//          for (var queue : queues)
//          {
//              // Create the tenant exchange.
//              if (!exchangeCreated) {
//                  try {channel.exchangeDeclare(exchangeName, "direct", durable, autodelete, exchangeArgs);}
//                      catch (Exception e) {
//                          String msg = LibUtils.getMsg("JOBS_QMGR_XCHG_ERROR", exchangeName,
//                                                        getOutConnectionName(), channel.getChannelNumber(),
//                                                        e.getMessage());
//                          throw new TapisQueueException(msg, e);
//                      }
//                  exchangeCreated = true; // one time flag
//              }
//
//              // Create and bind queue to exchange.
//              createAndBindQueue(channel, exchangeName, queue.getName());
//          }
//      }
//      finally {
//          // Close the channel if it exists and hasn't already been aborted.
//          if (channel != null)
//            try {channel.close();}
//                catch (Exception e1) {
//                    String msg = LibUtils.getMsg("JOBS_QMGR_CHANNEL_CLOSE_ERROR",
//                                                 channel.getChannelNumber(), e1.getMessage());
//                    log.warn(msg, e1);
//                }
//      }
//  }

  /**
   * Create the exchange and queue for notification events and bind them together
   */
  private void createNotifEventExchangeAndQueue() throws TapisQueueException
  {
    String service = _parms.getService();
    Channel channel = null;
    String bindingKey = DEFAULT_BINDING_KEY;
    Map<String, Object> exchangeArgs = null;
    try
    {
      // Create a temporary channel.
      channel = getNewInChannel();
      createExchangeAndQueue(channel, service, EXCHANGE_NAME, BuiltinExchangeType.TOPIC, QUEUE_NAME,
                             bindingKey, exchangeArgs);
    }
    finally
    {
      // Close the channel
      if (channel != null)
      {
        try
        {
          channel.close();
        }
        catch (Exception e)
        {
          String msg = LibUtils.getMsg("NTFLIB_MSGBRKR_CHAN_CLOSE_ERR2", channel.getChannelNumber(), e.getMessage());
          log.error(msg, e);
        }
      }
    }
  }

  /**
   * Establish the virtual host on the message broker.  All interactions
   * with the broker after this will be on the virtual host.  If the host
   * already exists and its administrator user has been granted the proper
   * permissions, then this method has no effect.
   *
   * @throws TapisRuntimeException on error
   */
  private void initRabbitVHost() throws TapisRuntimeException
  {
    RuntimeParameters rtParms = RuntimeParameters.getInstance();
    // Collect the runtime message broker information.
    var host  = _parms.getQueueHost();
    var user  = _parms.getQueueUser();
    var pass  = _parms.getQueuePassword();
    var vhost = _parms.getVhost();
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
}
