package edu.utexas.tacc.tapis.notifications.dao;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.Record;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.notifications.config.RuntimeParameters;
import edu.utexas.tacc.tapis.notifications.utils.LibUtils;
import edu.utexas.tacc.tapis.shared.exceptions.recoverable.TapisDBConnectionException;
import edu.utexas.tacc.tapis.shareddb.datasource.TapisDataSource;

//import edu.utexas.tacc.tapis.apps.model.App;
//import edu.utexas.tacc.tapis.apps.model.App.AppOperation;
//import edu.utexas.tacc.tapis.apps.model.App.Runtime;
//import edu.utexas.tacc.tapis.apps.model.App.RuntimeOption;
//import edu.utexas.tacc.tapis.apps.model.AppArg;
//import edu.utexas.tacc.tapis.apps.model.FileInput;
//import edu.utexas.tacc.tapis.apps.model.NotifMechanism;
//import edu.utexas.tacc.tapis.apps.model.NotifSubscription;
//import edu.utexas.tacc.tapis.apps.model.PatchApp;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;

import static edu.utexas.tacc.tapis.notifications.gen.jooq.Tables.SUBSCRIPTIONS;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;

import javax.sql.DataSource;

//import static edu.utexas.tacc.tapis.apps.model.App.INVALID_SEQ_ID;
//import static edu.utexas.tacc.tapis.apps.model.App.INVALID_UUID;
//import static edu.utexas.tacc.tapis.apps.model.App.NO_APP_VERSION;

/*
 * Class to handle persistence and queries for Tapis Notification resources.
 */
public class NotificationsDaoImpl2 implements NotificationsDao2
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Tracing.
  private static final Logger _log = LoggerFactory.getLogger(NotificationsDaoImpl2.class);

  private static final String VERS_ANY = "%";
  private static final String EMPTY_JSON = "{}";
  private static final String[] EMPTY_STR_ARRAY = {};

  // Create a static Set of column names for tables APPS and APPS_VERSIONS
  private static final Set<String> APPS_FIELDS = new HashSet<>();
  private static final Set<String> APPS_VERSIONS_FIELDS = new HashSet<>();
//  static
//  {
//    for (Field<?> field : APPS.fields()) { APPS_FIELDS.add(field.getName()); }
//    for (Field<?> field : APPS_VERSIONS.fields()) { APPS_VERSIONS_FIELDS.add(field.getName()); }
//  }

  // Compiled regex for splitting around "\."
  private static final Pattern DOT_SPLIT = Pattern.compile("\\.");

  // AND and OR operators
  private static final String AND = "AND";
  private static final String OR = "OR";

  /* ********************************************************************** */
  /*                             Public Methods                             */
  /* ********************************************************************** */

  /**
   * checkDB
   * Check that we can connect with DB and that the main table of the service exists.
   * @return null if all OK else return an exception
   */
  @Override
  public Exception checkDB()
  {
    Exception result = null;
    Connection conn = null;
    try
    {
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      // Build and execute a simple postgresql statement to check for the table
      String sql = "SELECT to_regclass('" + SUBSCRIPTIONS.getName() + "')";
      Result<Record> ret = db.resultQuery(sql).fetch();
      if (ret == null || ret.isEmpty() || ret.getValue(0,0) == null)
      {
        result = new TapisException(LibUtils.getMsg("APPLIB_CHECKDB_NO_TABLE", SUBSCRIPTIONS.getName()));
      }
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      result = e;
      // Rollback always logs msg and throws exception.
      // In this case of a simple check we ignore the exception, we just want the log msg
      try { LibUtils.rollbackDB(conn, e,"DB_DELETE_FAILURE", "notifications"); }
      catch (Exception e1) { }
    }
    finally
    {
      LibUtils.finalCloseDB(conn);
    }
    return result;
  }

  /**
   * migrateDB
   * Use Flyway to make sure DB schema is at the latest version
   */
  @Override
  public void migrateDB() throws TapisException
  {
    Flyway flyway = Flyway.configure().dataSource(getDataSource()).load();
    // TODO remove workaround if possible. Figure out how to deploy X.Y.Z-SNAPSHOT repeatedly.
    // Use repair() as workaround to avoid checksum error during develop/deploy of SNAPSHOT versions when it is not a true migration.
    flyway.repair();
    flyway.migrate();
  }

  /* ********************************************************************** */
  /*                             Private Methods                            */
  /* ********************************************************************** */

  /* ---------------------------------------------------------------------- */
  /* getConnection:                                                         */
  /* ---------------------------------------------------------------------- */
  /** Return a connection from the static datasource.  Create the datasource
   * on demand if it doesn't exist.
   *
   * @return a database connection
   * @throws TapisException on error
   */
  protected static synchronized Connection getConnection()
          throws TapisException
  {
    // Use the existing datasource.
    DataSource ds = getDataSource();
    // Get the connection.
    Connection conn;
    try {conn = ds.getConnection();}
    catch (Exception e) {
      String msg = MsgUtils.getMsg("DB_FAILED_CONNECTION");
      _log.error(msg, e);
      throw new TapisDBConnectionException(msg, e);
    }
    return conn;
  }

  /* ---------------------------------------------------------------------- */
  /* getDataSource:                                                         */
  /* ---------------------------------------------------------------------- */
  protected static DataSource getDataSource() throws TapisException
  {
    // Use the existing datasource.
    DataSource ds = TapisDataSource.getDataSource();
    if (ds == null) {
      try {
        // Get a database connection.
        RuntimeParameters parms = RuntimeParameters.getInstance();
        ds = TapisDataSource.getDataSource(parms.getInstanceName(),
                parms.getDbConnectionPoolName(),
                parms.getJdbcURL(),
                parms.getDbUser(),
                parms.getDbPassword(),
                parms.getDbConnectionPoolSize(),
                parms.getDbMeterMinutes());
      }
      catch (TapisException e) {
        // Details are already logged at exception site.
        String msg = MsgUtils.getMsg("DB_FAILED_DATASOURCE");
        _log.error(msg, e);
        throw new TapisException(msg, e);
      }
    }
    return ds;
  }
}
