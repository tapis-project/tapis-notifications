package edu.utexas.tacc.tapis.notifications.config;

import java.text.NumberFormat;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.runtime.TapisRuntimeException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.parameters.TapisEnv;
import edu.utexas.tacc.tapis.shared.parameters.TapisEnv.EnvVar;
import edu.utexas.tacc.tapis.shared.parameters.TapisInput;
import edu.utexas.tacc.tapis.shared.providers.email.EmailClientParameters;
import edu.utexas.tacc.tapis.shared.providers.email.enumeration.EmailProviderType;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.notifications.service.DispatchService;
import edu.utexas.tacc.tapis.notifications.utils.LibUtils;

/** This class contains the complete and effective set of runtime parameters
 * for this service.  Each service has it own version of this file that
 * contains the resolved values of configuration parameters needed to
 * initialize and run this service alone.  By resolved, we mean the values
 * assigned in this class are from the highest precedence source as
 * computed by TapisInput.  In addition, this class does not contain values 
 * used to initialize services other than the one in which it appears.
 * 
 * The getInstance() method of this singleton class will throw a runtime
 * exception if a required parameter is not provided or if any parameter
 * assignment fails, such as on a type conversion error.  This behavior
 * can be used to fail-fast services that are not configured correctly by
 * calling getInstance() early in a service's initialization sequence.
 */
public final class RuntimeParameters implements EmailClientParameters
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Tracing.
  private static final Logger _log = LoggerFactory.getLogger(RuntimeParameters.class);

  // Parameter defaults.
  private static final int CONNECTION_POOL_SIZE = 10;

  // Maximum size of a instance name string.
  private static final int MAX_INSTANCE_NAME_LEN = 26;

  // Default database metering interval in minutes.
  private static final int DEFAULT_DB_METER_INTERVAL_MINUTES = 60 * 24;

  // Email defaults.
  private static final String DEFAULT_EMAIL_PROVIDER = "LOG";
  private static final int    DEFAULT_EMAIL_PORT = 25;
  private static final String DEFAULT_EMAIL_FROM_NAME = "Tapis Notifications Service";
  private static final String DEFAULT_EMAIL_FROM_ADDRESS = "no-reply@nowhere.com";

  // Support defaults.
  private static final String DEFAULT_SUPPORT_NAME = "TACC";

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */
  // Singleton instance.
  private static RuntimeParameters _instance = initInstance();

  // Distinguished user-chosen name of this runtime instance.
  private String  instanceName;

  // Database configuration.
  private String  dbConnectionPoolName;
  private int     dbConnectionPoolSize;
  private String  dbUser;
  private String  dbPassword;
  private String  jdbcURL;
  private int     dbMeterMinutes;

  // Service config
  private String servicePassword;

  // Site on which we are running
  private String siteId;

  // Service base URLs - tenants
  private String tenantsSvcURL;

  // RabbitMQ configuration.
  private String  queueAdminUser;
  private String  queueAdminPassword;
  private int     queueAdminPort;
  private String  queueUser;
  private String  queuePassword;
  private String  queueHost;
  private int     queuePort;
  private boolean queueSSLEnabled;
  private boolean queueAutoRecoveryEnabled = true;

  // Mail configuration.
  private EmailProviderType emailProviderType;
  private boolean emailAuth;
  private String  emailHost;
  private int     emailPort;
  private String  emailUser;
  private String  emailPassword;
  private String  emailFromName;
  private String  emailFromAddress;

  // Support.
  private String  supportName;
  private String  supportEmail;

  // Allow test query parameters to be used.
  private boolean allowTestHeaderParms;

  // The slf4j/logback target directory and file.
  private String  logDirectory;
  private String  logFile;

  // TAPIS_NTF_DELIVERY_THREAD_POOL_SIZE
  private int ntfDeliveryThreadPoolSize = DispatchService.DEFAULT_NUM_DELIVERY_WORKERS;
  // TAPIS_NTF_SUBSCR_REAPER_INTERVAL (in minutes)
  private int ntfSubscriptionReaperInterval = DispatchService.DEFAULT_SUBSCR_REAPER_INTERVAL;

  // Number of attempts during initial delivery and interval (in seconds) between each one
  private int ntfDeliveryMaxAttempts = DispatchService.DEFAULT_DELIVERY_MAX_ATTEMPTS;
  private int  ntfDeliveryRetryInterval = DispatchService.DEFAULT_DELIVERY_RETRY_INTERVAL;

  // Number of attempts during recovery delivery and interval (in minutes) between each one
  private int ntfDeliveryRecoveryMaxAttempts = DispatchService.DEFAULT_DELIVERY_RCVRY_MAX_ATTEMPTS;
  private int  ntfDeliveryRecoveryRetryInterval = DispatchService.DEFAULT_DELIVERY_RCVRY_RETRY_INTERVAL;

  // TAPIS__LOCAL_TEST
  // Indicates we are running the service in TEST mode on localhost
  private boolean localTest = false;

  /* ********************************************************************** */
  /*                              Constructors                              */
  /* ********************************************************************** */
  /** This is where the work happens--either we can successfully create the
   * singleton object or we throw an exception which should abort service
   * initialization.  If an object is created, then all required input
   * parameters have been set in a syntactically valid way.
   *
   * @throws TapisRuntimeException on error
   */
  private RuntimeParameters() throws TapisRuntimeException
  {
      // Get env map vor EnvVar2 settings
      var envMap = System.getenv();

	  // --------------------- Get Input Parameters ---------------------
	  // Get the input parameter values from resource file and environment.
	  TapisInput tapisInput = new TapisInput(TapisConstants.SERVICE_NAME_NOTIFICATIONS);
	  Properties inputProperties;
	  try {inputProperties = tapisInput.getInputParameters();}
	  catch (TapisException e) {
	    // Very bad news.
	    String msg = MsgUtils.getMsg("TAPIS_SERVICE_INITIALIZATION_FAILED",
	                                 TapisConstants.SERVICE_NAME_NOTIFICATIONS,
	                                 e.getMessage());
	    _log.error(msg, e);
	    throw new TapisRuntimeException(msg, e);
	  }

	  // --------------------- Non-Configurable Parameters --------------
	  // We decide the pool name.
	  setDbConnectionPoolName(TapisConstants.SERVICE_NAME_NOTIFICATIONS + "Pool");
    
	  // --------------------- General Parameters -----------------------
	  // The name of this instance of the notifications library that has meaning to
	  // humans, distinguishes this instance of the job service, and is 
	  // short enough to use to name runtime artifacts.
	  String parm = inputProperties.getProperty(EnvVar.TAPIS_INSTANCE_NAME.getEnvName());
	  if (StringUtils.isBlank(parm)) {
	      // Default to some string that's not too long and somewhat unique.
	      // We check the current value to avoid reassigning on reload.  The
	      // integer suffix can add up to 10 characters to the string.
	      if (getInstanceName() == null)
	          setInstanceName(TapisConstants.SERVICE_NAME_NOTIFICATIONS +
                              Math.abs(new Random(System.currentTimeMillis()).nextInt()));
	  } 
	  else {
	      // Validate string length.
	      if (parm.length() > MAX_INSTANCE_NAME_LEN) {
	          String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_INITIALIZATION_FAILED",
                                           TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                                           "instanceName",
                                           "Instance name exceeds " + MAX_INSTANCE_NAME_LEN + "characters: " + parm);
	          _log.error(msg);
	          throw new TapisRuntimeException(msg);
      }
      if (!StringUtils.isAlphanumeric(parm)) {
              String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_INITIALIZATION_FAILED",
                                           TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                                           "instanceName",
                                           "Instance name contains non-alphanumeric characters: " + parm);
              _log.error(msg);
              throw new TapisRuntimeException(msg);
      }
    }
	
    // Location of log file
    parm = inputProperties.getProperty(EnvVar.TAPIS_LOG_DIRECTORY.getEnvName());
    if (!StringUtils.isBlank(parm)) setLogDirectory(parm);
    parm = inputProperties.getProperty(EnvVar.TAPIS_LOG_FILE.getEnvName());
    if (!StringUtils.isBlank(parm)) setLogFile(parm);
                 
    // Optional test header parameter switch.
    parm = inputProperties.getProperty(EnvVar.TAPIS_ENVONLY_ALLOW_TEST_HEADER_PARMS.getEnvName());
    if (StringUtils.isBlank(parm)) setAllowTestHeaderParms(false);
      else {
        try {setAllowTestHeaderParms(Boolean.parseBoolean(parm));}
          catch (Exception e) {
            // Stop on bad input.
            String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_INITIALIZATION_FAILED",
                                         TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                                         "allowTestQueryParms",
                                         e.getMessage());
            _log.error(msg, e);
            throw new TapisRuntimeException(msg, e);
          }
      }

		// --------------------- Service config --------------------------------
		parm = inputProperties.getProperty(EnvVar.TAPIS_SERVICE_PASSWORD.getEnvName());
		if (!StringUtils.isBlank(parm)) setServicePassword(parm);

      // --------------------- Site on which we are running ----------------------------
      // Site is required. Throw runtime exception if not found.
      parm = inputProperties.getProperty(EnvVar.TAPIS_SITE_ID.getEnvName());
      if (StringUtils.isBlank(parm)) {
        String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_MISSING", TapisConstants.SERVICE_NAME_NOTIFICATIONS, "siteId");
        _log.error(msg);
        throw new TapisRuntimeException(msg);
      }
      setSiteId(parm);

      // --------------------- Base URLs for other services that this service requires ----------------------------
		// Tenants service base URL is required. Throw runtime exception if not found.
		// Security Kernel base URL is optional. Normally it is retrieved from the Tenants service.
		parm = inputProperties.getProperty(EnvVar.TAPIS_TENANT_SVC_BASEURL.getEnvName());
		if (StringUtils.isBlank(parm)) {
			String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_MISSING", TapisConstants.SERVICE_NAME_NOTIFICATIONS, "tenantsSvcUrl");
			_log.error(msg);
			throw new TapisRuntimeException(msg);
		}
		setTenantsSvcURL(parm);

	// --------------------- DB Parameters ----------------------------
    // User does not have to provide a pool size.
    parm = inputProperties.getProperty(EnvVar.TAPIS_DB_CONNECTION_POOL_SIZE.getEnvName());
    if (StringUtils.isBlank(parm)) setDbConnectionPoolSize(CONNECTION_POOL_SIZE);
      else {
        try {setDbConnectionPoolSize(Integer.parseInt(parm));}
          catch (Exception e) {
            // Stop on bad input.
            String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_INITIALIZATION_FAILED",
                                         TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                                         "dbConnectionPoolSize",
                                         e.getMessage());
            _log.error(msg, e);
            throw new TapisRuntimeException(msg, e);
          }
      }
    
    // DB user is required.
    parm = inputProperties.getProperty(EnvVar.TAPIS_DB_USER.getEnvName());
    if (StringUtils.isBlank(parm)) {
      // Stop on bad input.
      String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_MISSING",
                                   TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                                   "dbUser");
      _log.error(msg);
      throw new TapisRuntimeException(msg);
    }
    setDbUser(parm);

    // DB user password is required.
    parm = inputProperties.getProperty(EnvVar.TAPIS_DB_PASSWORD.getEnvName());
    if (StringUtils.isBlank(parm)) {
      // Stop on bad input.
      String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_MISSING",
                                   TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                                   "dbPassword");
      _log.error(msg);
      throw new TapisRuntimeException(msg);
    }
    setDbPassword(parm);
    
    // JDBC url is required.
    parm = inputProperties.getProperty(EnvVar.TAPIS_DB_JDBC_URL.getEnvName());
    if (StringUtils.isBlank(parm)) {
      // Stop on bad input.
      String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_MISSING",
                                   TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                                   "jdbcUrl");
      _log.error(msg);
      throw new TapisRuntimeException(msg);
    }
    setJdbcURL(parm);

    // Specify zero or less minutes to turn off database metering.
    parm = inputProperties.getProperty(EnvVar.TAPIS_DB_METER_MINUTES.getEnvName());
    if (StringUtils.isBlank(parm)) setDbMeterMinutes(DEFAULT_DB_METER_INTERVAL_MINUTES);
      else {
        try {setDbConnectionPoolSize(Integer.parseInt(parm));}
          catch (Exception e) {
            // Stop on bad input.
            String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_INITIALIZATION_FAILED",
                                         TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                                         "dbMeterMinutes",
                                         e.getMessage());
            _log.error(msg, e);
            throw new TapisRuntimeException(msg, e);
          }
      }

      // --------------------- RabbitMQ Parameters ----------------------
      // The broker's administrator credentials used to set up vhost.
      parm = inputProperties.getProperty(EnvVar.TAPIS_QUEUE_ADMIN_USER.getEnvName());
      if (!StringUtils.isBlank(parm)) setQueueAdminUser(parm);
      else {
        // Stop on bad input.
        String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_MISSING",
                TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                "queueAdminUser");
        _log.error(msg);
        throw new TapisRuntimeException(msg);
      }
      parm = inputProperties.getProperty(EnvVar.TAPIS_QUEUE_ADMIN_PASSWORD.getEnvName());
      if (!StringUtils.isBlank(parm)) setQueueAdminPassword(parm);
      else {
        // Stop on bad input.
        String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_MISSING",
                TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                "queueAdminPassword");
        _log.error(msg);
        throw new TapisRuntimeException(msg);
      }

      // Optional broker port.
      parm = inputProperties.getProperty(EnvVar.TAPIS_QUEUE_ADMIN_PORT.getEnvName());
      if (StringUtils.isBlank(parm)) setQueueAdminPort(isQueueSSLEnabled() ? 15671 : 15672);
      else {
        try {setQueueAdminPort(Integer.parseInt(parm));}
        catch (Exception e) {
          // Stop on bad input.
          String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_INITIALIZATION_FAILED",
                  TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                  "queuePort",
                  e.getMessage());
          _log.error(msg, e);
          throw new TapisRuntimeException(msg, e);
        }
      }

      // This service's normal runtime credentials.
      parm = inputProperties.getProperty(EnvVar.TAPIS_QUEUE_USER.getEnvName());
      if (!StringUtils.isBlank(parm)) setQueueUser(parm);
      else {
        // Stop on bad input.
        String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_MISSING",
                TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                "queueUser");
        _log.error(msg);
        throw new TapisRuntimeException(msg);
      }
      parm = inputProperties.getProperty(EnvVar.TAPIS_QUEUE_PASSWORD.getEnvName());
      if (!StringUtils.isBlank(parm)) setQueuePassword(parm);
      else {
        // Stop on bad input.
        String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_MISSING",
                TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                "queuePassword");
        _log.error(msg);
        throw new TapisRuntimeException(msg);
      }

      // Optional ssl enabled.  Compute this value before assigning a default port.
      parm = inputProperties.getProperty(EnvVar.TAPIS_QUEUE_SSL_ENABLE.getEnvName());
      if (StringUtils.isBlank(parm)) setQueueSSLEnabled(false);
      else {
        try {setQueueSSLEnabled(Boolean.parseBoolean(parm));}
        catch (Exception e) {
          // Stop on bad input.
          String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_INITIALIZATION_FAILED",
                  TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                  "queueSSLEnabled",
                  e.getMessage());
          _log.error(msg, e);
          throw new TapisRuntimeException(msg, e);
        }
      }

      // Broker host defaults to localhost.
      parm = inputProperties.getProperty(EnvVar.TAPIS_QUEUE_HOST.getEnvName());
      if (!StringUtils.isBlank(parm)) setQueueHost(parm);
      else setQueueHost("localhost");

      // Optional broker port.
      parm = inputProperties.getProperty(EnvVar.TAPIS_QUEUE_PORT.getEnvName());
      if (StringUtils.isBlank(parm))
        setQueuePort(isQueueSSLEnabled() ? ConnectionFactory.DEFAULT_AMQP_OVER_SSL_PORT :
                ConnectionFactory.DEFAULT_AMQP_PORT);
      else {
        try {setQueuePort(Integer.parseInt(parm));}
        catch (Exception e) {
          // Stop on bad input.
          String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_INITIALIZATION_FAILED",
                  TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                  "queuePort",
                  e.getMessage());
          _log.error(msg, e);
          throw new TapisRuntimeException(msg, e);
        }
      }

      // Optional auto-recovery enabled by default.
      parm = inputProperties.getProperty(EnvVar.TAPIS_QUEUE_AUTO_RECOVERY.getEnvName());
      if (StringUtils.isBlank(parm)) setQueueAutoRecoveryEnabled(true);
      else {
        try {setQueueAutoRecoveryEnabled(Boolean.parseBoolean(parm));}
        catch (Exception e) {
          // Stop on bad input.
          String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_INITIALIZATION_FAILED",
                  TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                  "queueAutoRecoveryEnabled",
                  e.getMessage());
          _log.error(msg, e);
          throw new TapisRuntimeException(msg, e);
        }
      }

    //  ntfDeliveryThreadPoolSize
      parm = envMap.get(EnvVar2.TAPIS_NTF_DELIVERY_THREAD_POOL_SIZE.name());
      int parmInt = DispatchService.DEFAULT_NUM_DELIVERY_WORKERS;
      // If parameter is set attempt to parse it as an integer
      if (!StringUtils.isBlank(parm))
      {
        try { parmInt = Integer.parseInt(parm); }
        catch (NumberFormatException e)
        {
          // Log error and stop
          String msg = LibUtils.getMsg("NTFLIB_RUNTIME_NUM_PARSE_FAIL", EnvVar2.TAPIS_NTF_DELIVERY_THREAD_POOL_SIZE, parm);
          _log.error(msg, e);
          throw new TapisRuntimeException(msg, e);
        }
      }
      setNtfDeliveryThreadPoolSize(parmInt);

      //  ntfSubscriptionReaperInterval
      parm = envMap.get(EnvVar2.TAPIS_NTF_SUBSCR_REAPER_INTERVAL.name());
      parmInt = DispatchService.DEFAULT_SUBSCR_REAPER_INTERVAL;
      // If parameter is set attempt to parse it as an integer
      if (!StringUtils.isBlank(parm))
      {
        try { parmInt = Integer.parseInt(parm); }
        catch (NumberFormatException e)
        {
          // Log error and stop
          String msg = LibUtils.getMsg("NTFLIB_RUNTIME_NUM_PARSE_FAIL", EnvVar2.TAPIS_NTF_SUBSCR_REAPER_INTERVAL, parm);
          _log.error(msg, e);
          throw new TapisRuntimeException(msg, e);
        }
      }
      setNtfSubscriptionReaperInterval(parmInt);

      //  ntfDeliveryAttempts
      parm = envMap.get(EnvVar2.TAPIS_NTF_DELIVERY_ATTEMPTS.name());
      parmInt = DispatchService.DEFAULT_DELIVERY_MAX_ATTEMPTS;
      // If parameter is set attempt to parse it as an integer
      if (!StringUtils.isBlank(parm))
      {
        try { parmInt = Integer.parseInt(parm); }
        catch (NumberFormatException e)
        {
          // Log error and stop
          String msg = LibUtils.getMsg("NTFLIB_RUNTIME_NUM_PARSE_FAIL", EnvVar2.TAPIS_NTF_DELIVERY_ATTEMPTS, parm);
          _log.error(msg, e);
          throw new TapisRuntimeException(msg, e);
        }
      }
      setNtfDeliveryMaxAttempts(parmInt);

      //  ntfDeliveryRetryInterval
      parm = envMap.get(EnvVar2.TAPIS_NTF_DELIVERY_RETRY_INTERVAL.name());
      parmInt = DispatchService.DEFAULT_DELIVERY_RETRY_INTERVAL;
      // If parameter is set attempt to parse it as an integer
      if (!StringUtils.isBlank(parm))
      {
        try { parmInt = Integer.parseInt(parm); }
        catch (NumberFormatException e)
        {
          // Log error and stop
          String msg = LibUtils.getMsg("NTFLIB_RUNTIME_NUM_PARSE_FAIL", EnvVar2.TAPIS_NTF_DELIVERY_RETRY_INTERVAL, parm);
          _log.error(msg, e);
          throw new TapisRuntimeException(msg, e);
        }
      }
      setNtfDeliveryRetryInterval(parmInt);

      //  ntfDeliveryRecoveryAttempts
      parm = envMap.get(EnvVar2.TAPIS_NTF_DELIVERY_RCVRY_ATTEMPTS.name());
      parmInt = DispatchService.DEFAULT_DELIVERY_RCVRY_MAX_ATTEMPTS;
      // If parameter is set attempt to parse it as an integer
      if (!StringUtils.isBlank(parm))
      {
        try { parmInt = Integer.parseInt(parm); }
        catch (NumberFormatException e)
        {
          // Log error and stop
          String msg = LibUtils.getMsg("NTFLIB_RUNTIME_NUM_PARSE_FAIL", EnvVar2.TAPIS_NTF_DELIVERY_RCVRY_ATTEMPTS, parm);
          _log.error(msg, e);
          throw new TapisRuntimeException(msg, e);
        }
      }
      setNtfDeliveryRecoveryMaxAttempts(parmInt);

      //  ntfDeliveryRecoveryRetryInterval
      parm = envMap.get(EnvVar2.TAPIS_NTF_DELIVERY_RCVRY_RETRY_INTERVAL.name());
      parmInt = DispatchService.DEFAULT_DELIVERY_RCVRY_RETRY_INTERVAL;
      // If parameter is set attempt to parse it as an integer
      if (!StringUtils.isBlank(parm))
      {
        try { parmInt = Integer.parseInt(parm); }
        catch (NumberFormatException e)
        {
          // Log error and stop
          String msg = LibUtils.getMsg("NTFLIB_RUNTIME_NUM_PARSE_FAIL", EnvVar2.TAPIS_NTF_DELIVERY_RCVRY_RETRY_INTERVAL, parm);
          _log.error(msg, e);
          throw new TapisRuntimeException(msg, e);
        }
      }
      setNtfDeliveryRecoveryRetryInterval(parmInt);

      // Optional flag indicating we are running in local test mode
      parm = envMap.get(EnvVar2.TAPIS_LOCAL_TEST.name());
      if (StringUtils.isBlank(parm)) setLocalTest(false);
      else {
        try {
          setLocalTest(Boolean.parseBoolean(parm));}
        catch (Exception e) {
          // Stop on bad input.
          String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_INITIALIZATION_FAILED",
                  TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                  "localTestFlag",
                  e.getMessage());
          _log.error(msg, e);
          throw new TapisRuntimeException(msg, e);
        }
      }

      // --------------------- Email Parameters -------------------------
    // Currently LOG or SMTP.
    parm = inputProperties.getProperty(EnvVar.TAPIS_MAIL_PROVIDER.getEnvName());
    if (StringUtils.isBlank(parm)) parm = DEFAULT_EMAIL_PROVIDER;
    try {setEmailProviderType(EmailProviderType.valueOf(parm));}
        catch (Exception e) {
            String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_INITIALIZATION_FAILED",
                                         TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                                         "emalProviderType",
                                         e.getMessage());
            _log.error(msg, e);
            throw new TapisRuntimeException(msg, e);
        }
    
    // Is authentication required?
    parm = inputProperties.getProperty(EnvVar.TAPIS_SMTP_AUTH.getEnvName());
    if (StringUtils.isBlank(parm)) setEmailAuth(false);
      else {
          try {setEmailAuth(Boolean.parseBoolean(parm));}
              catch (Exception e) {
                  // Stop on bad input.
                  String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_INITIALIZATION_FAILED",
                                               TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                                               "emailAuth",
                                               e.getMessage());
                  _log.error(msg, e);
                  throw new TapisRuntimeException(msg, e);
              }
      }
    
    // Get the email server host.
    parm = inputProperties.getProperty(EnvVar.TAPIS_SMTP_HOST.getEnvName());
    if (!StringUtils.isBlank(parm)) setEmailHost(parm);
      else if (getEmailProviderType() == EmailProviderType.SMTP) {
          String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_MISSING",
                                       TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                                       "emailHost");
          _log.error(msg);
          throw new TapisRuntimeException(msg);
      }
        
    // Get the email server port.
    parm = inputProperties.getProperty(EnvVar.TAPIS_SMTP_PORT.getEnvName());
    if (StringUtils.isBlank(parm)) setEmailPort(DEFAULT_EMAIL_PORT);
      else
        try {setEmailPort(Integer.parseInt(parm));}
          catch (Exception e) {
              // Stop on bad input.
              String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_INITIALIZATION_FAILED",
                                           TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                                           "emailPort",
                                           e.getMessage());
              _log.error(msg, e);
              throw new TapisRuntimeException(msg, e);
          }

    // Get the email user, for auth.
    parm = inputProperties.getProperty(EnvVar.TAPIS_SMTP_USER.getEnvName());
    if (!StringUtils.isBlank(parm)) setEmailUser(parm);
      else if (isEmailAuth()) {
          String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_MISSING",
                                       TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                                       "emailUser");
          _log.error(msg);
          throw new TapisRuntimeException(msg);
      }
        
    // Get the email password, for auth.
    parm = inputProperties.getProperty(EnvVar.TAPIS_SMTP_PASSWORD.getEnvName());
    if (!StringUtils.isBlank(parm)) setEmailPassword(parm);
      else if (isEmailAuth()) {
        String msg = MsgUtils.getMsg("TAPIS_SERVICE_PARM_MISSING",
                                     TapisConstants.SERVICE_NAME_NOTIFICATIONS,
                                     "emailPassword");
        _log.error(msg);
        throw new TapisRuntimeException(msg);
      }
        
    // Get the email name for the From: field
    parm = inputProperties.getProperty(EnvVar.TAPIS_SMTP_FROM_NAME.getEnvName());
    if (!StringUtils.isBlank(parm)) setEmailFromName(parm);
      else setEmailFromName(DEFAULT_EMAIL_FROM_NAME);
        
    // Get the email address for the From: field
    parm = inputProperties.getProperty(EnvVar.TAPIS_SMTP_FROM_ADDRESS.getEnvName());
    if (!StringUtils.isBlank(parm)) setEmailFromAddress(parm);
      else setEmailFromAddress(DEFAULT_EMAIL_FROM_ADDRESS);
    
    // --------------------- Support Parameters -----------------------
    // Chose a name for support or one will be chosen.
    parm = inputProperties.getProperty(EnvVar.TAPIS_SUPPORT_NAME.getEnvName());
    if (!StringUtils.isBlank(parm)) setSupportName(parm);
     else setSupportName(DEFAULT_SUPPORT_NAME);
    
    // Empty support email means no support emails will be sent.
    parm = inputProperties.getProperty(EnvVar.TAPIS_SUPPORT_EMAIL.getEnvName());
    if (!StringUtils.isBlank(parm)) setSupportEmail(parm);
  }
	
  /*
   * Return listing of runtime settings as well as some OS and JVM information.
   */
  public String getRuntimeParameters()
  {
    var buf = new StringBuilder();
    buf.append("======================");
    buf.append("\nRuntime Parameters");
    buf.append("\n======================");
    buf.append("\n------- Service Specific -------------------------------");
    buf.append("\ntapis.ntf.delivery.thread.pool.size: ").append(getNtfDeliveryThreadPoolSize());
    buf.append("\ntapis.ntf.subscription.reaper.interval: ").append(getNtfSubscriptionReaperInterval());
    buf.append("\ntapis.ntf.delivery.attempts: ").append(getNtfDeliveryMaxAttempts());
    buf.append("\ntapis.ntf.delivery.retry.interval: ").append(getNtfDeliveryRetryInterval());
    buf.append("\ntapis.ntf.delivery.rcvry.attempts: ").append(getNtfDeliveryRecoveryMaxAttempts());
    buf.append("\ntapis.ntf.delivery.rcvry.retry.interval: ").append(getNtfDeliveryRecoveryRetryInterval());
    buf.append("\ntapis.local.test: ").append(isLocalTest());
    buf.append("\n------- Logging -----------------------------------");
    buf.append("\ntapis.log.directory: ");
    buf.append(this.getLogDirectory());
    buf.append("\ntapis.log.file: ");
    buf.append(this.getLogFile());

    buf.append("\n------- Network -----------------------------------");
    buf.append("\nHost Addresses: ");
    buf.append(getNetworkAddresses());

    buf.append("\n------- DB Configuration --------------------------");
    buf.append("\ntapis.db.jdbc.url: ");
    buf.append(this.getJdbcURL());
    buf.append("\ntapis.db.user: ");
    buf.append(this.getDbUser());
    buf.append("\ntapis.db.connection.pool.size: ");
    buf.append(this.getDbConnectionPoolSize());
    buf.append("\ntapis.db.meter.minutes: ");
    buf.append(this.getDbMeterMinutes());

    buf.append("\n------- Site Id --------------------------");
    buf.append("\ntapis.site.id: ");
    buf.append(siteId);

    buf.append("\n------- Base Service URLs --------------------------");
    buf.append("\ntapis.svc.tenants.url: ");
    buf.append(tenantsSvcURL);

    buf.append("\n------- RabbitMQ Configuration --------------------");
    buf.append("\ntapis.queue.host: ");
    buf.append(this.getQueueHost());
    buf.append("\ntapis.queue.admin.user: ");
    buf.append(this.getQueueAdminUser());
    buf.append("\ntapis.queue.admin.port: ");
    buf.append(this.getQueueAdminPort());
    buf.append("\ntapis.queue.user: ");
    buf.append(this.getQueueUser());
    buf.append("\ntapis.queue.port: ");
    buf.append(this.getQueuePort());
    buf.append("\ntapis.queue.ssl.enable: ");
    buf.append(this.isQueueSSLEnabled());
    buf.append("\ntapis.queue.auto.recovery: ");
    buf.append(this.isQueueAutoRecoveryEnabled());

    buf.append("\n------- Email Configuration -----------------------");
    buf.append("\ntapis.mail.provider: ");
    buf.append(this.getEmailProviderType().name());
    buf.append("\ntapis.smtp.auth: ");
    buf.append(this.isEmailAuth());
    buf.append("\ntapis.smtp.host: ");
    buf.append(this.getEmailHost());
    buf.append("\ntapis.smtp.port: ");
    buf.append(this.getEmailPort());
    buf.append("\ntapis.smtp.user: ");
    buf.append(this.getEmailUser());
    buf.append("\ntapis.smtp.from.name: ");
    buf.append(this.getEmailFromName());
    buf.append("\ntapis.smtp.from.address: ");
    buf.append(this.getEmailFromAddress());

    buf.append("\n------- Support Configuration ---------------------");
    buf.append("\ntapis.support.name: ");
    buf.append(this.getSupportName());
    buf.append("\ntapis.support.email: ");
    buf.append(this.getSupportEmail());

    buf.append("\n------- EnvOnly Configuration ---------------------");
    buf.append("\ntapis.envonly.log.security.info: ");
    buf.append(RuntimeParameters.getLogSecurityInfo());
    buf.append("\ntapis.envonly.allow.test.header.parms: ");
    buf.append(this.isAllowTestHeaderParms());
    buf.append("\ntapis.envonly.jwt.optional: ");
    buf.append(TapisEnv.getBoolean(EnvVar.TAPIS_ENVONLY_JWT_OPTIONAL));
    buf.append("\ntapis.envonly.skip.jwt.verify: ");
    buf.append(TapisEnv.getBoolean(EnvVar.TAPIS_ENVONLY_SKIP_JWT_VERIFY));

    buf.append("\n------- Java Configuration ------------------------");
    buf.append("\njava.version: ");
    buf.append(System.getProperty("java.version"));
    buf.append("\njava.vendor: ");
    buf.append(System.getProperty("java.vendor"));
    buf.append("\njava.vm.version: ");
    buf.append(System.getProperty("java.vm.version"));
    buf.append("\njava.vm.vendor: ");
    buf.append(System.getProperty("java.vm.vendor"));
    buf.append("\njava.vm.name: ");
    buf.append(System.getProperty("java.vm.name"));
    buf.append("\nos.name: ");
    buf.append(System.getProperty("os.name"));
    buf.append("\nos.arch: ");
    buf.append(System.getProperty("os.arch"));
    buf.append("\nos.version: ");
    buf.append(System.getProperty("os.version"));
    buf.append("\nuser.name: ");
    buf.append(System.getProperty("user.name"));
    buf.append("\nuser.home: ");
    buf.append(System.getProperty("user.home"));
    buf.append("\nuser.dir: ");
    buf.append(System.getProperty("user.dir"));

    buf.append("\n------- JVM Runtime Values ------------------------");
    NumberFormat formatter = NumberFormat.getIntegerInstance();
    buf.append("\navailableProcessors: ");
    buf.append(formatter.format(Runtime.getRuntime().availableProcessors()));
    buf.append("\nmaxMemory: ");
    buf.append(formatter.format(Runtime.getRuntime().maxMemory()));
    buf.append("\ntotalMemory: ");
    buf.append(formatter.format(Runtime.getRuntime().totalMemory()));
    buf.append("\nfreeMemory: ");
    buf.append(formatter.format(Runtime.getRuntime().freeMemory()));

    buf.append("\n\n");
    return buf.toString();
  }

    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
	/* ---------------------------------------------------------------------- */
	/* initInstance:                                                          */
	/* ---------------------------------------------------------------------- */
	/** Initialize the singleton instance of this class.
	 * 
	 * @return the non-null singleton instance of this class
	 */
	private static synchronized RuntimeParameters initInstance()
	{
		if (_instance == null) _instance = new RuntimeParameters();
		return _instance;
	}
	
    /* ---------------------------------------------------------------------- */
    /* getNetworkAddresses:                                                   */
    /* ---------------------------------------------------------------------- */
	/** Best effort attempt to get the network addresses of this host for 
	 * logging purposes.
	 * 
	 * @return the comma separated string of IP addresses or null
	 */
    private String getNetworkAddresses()
    {
        // Comma separated result string.
        String addresses = null;
        
        // Best effort attempt to get this host's ip addresses.
        try {
            List<String> list = TapisUtils.getIpAddressesFromNetInterface();
            if (!list.isEmpty()) { 
                String[] array = new String[list.size()];
                array = list.toArray(array);
                addresses = String.join(", ", array);
            }
        }
        catch (Exception e) {/* ignore exceptions */}
        
        // Can be null.
        return addresses;
    }
    
	/* ********************************************************************** */
	/*                             Public Methods                             */
	/* ********************************************************************** */
	/* ---------------------------------------------------------------------- */
	/* reload:                                                                */
	/* ---------------------------------------------------------------------- */
	/** Reload the parameters from scratch.  Should not be called too often,
	 * but does allow updates to parameter files and environment variables
	 * to be recognized.  
	 * 
	 * Note that concurrent calls to getInstance() will either return the 
	 * new or old parameters object, but whichever is returned it will be
	 * consistent.  Calls to specific parameter methods will also be 
	 * consistent, but the instance on which they are called may be stale
	 * if it was acquired before the last reload operation.  
	 * 
	 * @return a new instance of the runtime parameters
	 */
	public static synchronized RuntimeParameters reload()
	{
	  _instance = new RuntimeParameters();
	  return _instance;
	}
	
	/* ---------------------------------------------------------------------- */
	/* getLogSecurityInfo:                                                    */
	/* ---------------------------------------------------------------------- */
	/** Go directly to the environment to get the latest security info logging
	 * value.  This effectively disregards any setting the appears in a 
	 * properties file or on the JVM command line.
	 * 
	 * @return the current environment variable setting 
	 */
	public static boolean getLogSecurityInfo()
	{
	    // Always return the latest environment value.
	    return TapisEnv.getLogSecurityInfo();
	}
  
    /* ********************************************************************** */
    /*                               Accessors                                */
    /* ********************************************************************** */
	public static RuntimeParameters getInstance() {
		return _instance;
	}

	public String getDbConnectionPoolName() {
		return dbConnectionPoolName;
	}
	private void setDbConnectionPoolName(String dbConnectionPoolName) {
		this.dbConnectionPoolName = dbConnectionPoolName;
	}

	public int getDbConnectionPoolSize() {
		return dbConnectionPoolSize;
	}
	private void setDbConnectionPoolSize(int dbConnectionPoolSize) {
		this.dbConnectionPoolSize = dbConnectionPoolSize;
	}

	public String getDbUser() {
		return dbUser;
	}
	private void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}

	public String getDbPassword() {
		return dbPassword;
	}
	private void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}

	public String getJdbcURL() {
		return jdbcURL;
	}
	private void setJdbcURL(String jdbcURL) {
		this.jdbcURL = jdbcURL;
	}

	public String getServicePassword() { return servicePassword; }
	private void setServicePassword(String p) {servicePassword = p; }

  public String getSiteId() { return siteId; }
  private void setSiteId(String s) {siteId = s; }

    public String getTenantsSvcURL() { return tenantsSvcURL; }
	private void setTenantsSvcURL(String url) {tenantsSvcURL = url; }

	public String getInstanceName() {
	    return instanceName;
	}
	private void setInstanceName(String s) { instanceName = s; }

	public boolean isAllowTestHeaderParms() {
	    return allowTestHeaderParms;
	}
	private void setAllowTestHeaderParms(boolean b) {   allowTestHeaderParms = b; }

	public int getDbMeterMinutes() {
	    return dbMeterMinutes;
	}
	private void setDbMeterMinutes(int dbMeterMinutes) {
	    this.dbMeterMinutes = dbMeterMinutes;
	}

  public String getQueueAdminUser() {
    return queueAdminUser;
  }
  public void setQueueAdminUser(String s) {
    queueAdminUser = s;
  }

  public String getQueueAdminPassword() {
    return queueAdminPassword;
  }
  public void setQueueAdminPassword(String s) {
    queueAdminPassword = s;
  }

  public int getQueueAdminPort() {
    return queueAdminPort;
  }
  public void setQueueAdminPort(int s) {
    queueAdminPort = s;
  }

  public String getQueueUser() {
    return queueUser;
  }
  public void setQueueUser(String s) {
    queueUser = s;
  }

  public String getQueuePassword() {
    return queuePassword;
  }
  public void setQueuePassword(String s) {
    queuePassword = s;
  }

  public String getQueueHost() { return queueHost; }
  public void setQueueHost(String s) {
    queueHost = s;
  }

  public int getQueuePort() {
    return queuePort;
  }
  public void setQueuePort(int i) {
    queuePort = i;
  }

  public boolean isQueueSSLEnabled() {
    return queueSSLEnabled;
  }
  public void setQueueSSLEnabled(boolean queueSSLEnabled) {
    this.queueSSLEnabled = queueSSLEnabled;
  }

  public boolean isQueueAutoRecoveryEnabled() {
    return queueAutoRecoveryEnabled;
  }
  public void setQueueAutoRecoveryEnabled(boolean b) { queueAutoRecoveryEnabled = b; }

    public EmailProviderType getEmailProviderType() {
        return emailProviderType;
    }
    public void setEmailProviderType(EmailProviderType e) { emailProviderType = e; }

    public boolean isEmailAuth() {
        return emailAuth;
    }
    public void setEmailAuth(boolean emailAuth) {
        this.emailAuth = emailAuth;
    }

    public String getEmailHost() {
        return emailHost;
    }
    public void setEmailHost(String emailHost) {
        this.emailHost = emailHost;
    }

    public int getEmailPort() {
        return emailPort;
    }
    public void setEmailPort(int emailPort) {
        this.emailPort = emailPort;
    }

    public String getEmailUser() {
        return emailUser;
    }
    public void setEmailUser(String emailUser) {
        this.emailUser = emailUser;
    }

    public String getEmailPassword() {
        return emailPassword;
    }
    public void setEmailPassword(String emailPassword) {
        this.emailPassword = emailPassword;
    }

    public String getEmailFromName() {
        return emailFromName;
    }
    public void setEmailFromName(String emailFromName) {
        this.emailFromName = emailFromName;
    }

    public String getEmailFromAddress() {
        return emailFromAddress;
    }
    public void setEmailFromAddress(String emailFromAddress) {
        this.emailFromAddress = emailFromAddress;
    }

    public String getSupportName() {
        return supportName;
    }
    public void setSupportName(String supportName) {
        this.supportName = supportName;
    }

    public String getSupportEmail() {
        return supportEmail;
    }
    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public String getLogDirectory() {
        return logDirectory;
    }
    public void setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory;
    }

    public String getLogFile() {
        return logFile;
    }
    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

  // property TAPIS_NTF_DELIVERY_THREAD_POOL_SIZE
  public int getNtfDeliveryThreadPoolSize() { return ntfDeliveryThreadPoolSize; }
  private void setNtfDeliveryThreadPoolSize(int i) { ntfDeliveryThreadPoolSize = i; }
  // property TAPIS_NTF_SUBSCR_REAPER_INTERVAL
  public int getNtfSubscriptionReaperInterval() { return ntfSubscriptionReaperInterval; }
  private void setNtfSubscriptionReaperInterval(int i) { ntfSubscriptionReaperInterval = i; }
  // property TAPIS_NTF_DELIVERY_ATTEMPTS
  public int getNtfDeliveryMaxAttempts() { return ntfDeliveryMaxAttempts; }
  private void setNtfDeliveryMaxAttempts(int i) { ntfDeliveryMaxAttempts = i; }
  // property TAPIS_NTF_DELIVERY_RETRY_INTERVAL
  public int getNtfDeliveryRetryInterval() { return ntfDeliveryRetryInterval; }
  private void setNtfDeliveryRetryInterval(int i) { ntfDeliveryRetryInterval = i; }
  // property TAPIS_NTF_DELIVERY_RCVRY_ATTEMPTS
  public int getNtfDeliveryRecoveryMaxAttempts() { return ntfDeliveryRecoveryMaxAttempts; }
  private void setNtfDeliveryRecoveryMaxAttempts(int i) { ntfDeliveryRecoveryMaxAttempts = i; }
  // property TAPIS_NTF_DELIVERY_RCVRY_RETRY_INTERVAL
  public int getNtfDeliveryRecoveryRetryInterval() { return ntfDeliveryRecoveryRetryInterval; }
  private void setNtfDeliveryRecoveryRetryInterval(int i) { ntfDeliveryRecoveryRetryInterval = i; }

  // property TAPIS_NTF_LOCAL_TEST_FLAG
  // Indicates we are running the service in TEST mode on localhost
  public boolean isLocalTest() { return localTest; }
  private void setLocalTest(boolean b) { localTest = b; }

  /*
   * Env variables specific to this service
   * Note they always use format of uppercase with underscores.
   * Setting via properties file or using lowercase with dots not supported
   */
  private enum EnvVar2
  {
    TAPIS_NTF_DELIVERY_THREAD_POOL_SIZE,
    TAPIS_NTF_SUBSCR_REAPER_INTERVAL,
    TAPIS_NTF_DELIVERY_ATTEMPTS,
    TAPIS_NTF_DELIVERY_RETRY_INTERVAL,
    TAPIS_NTF_DELIVERY_RCVRY_ATTEMPTS,
    TAPIS_NTF_DELIVERY_RCVRY_RETRY_INTERVAL,
    TAPIS_LOCAL_TEST}
}
