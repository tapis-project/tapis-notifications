package edu.utexas.tacc.tapis.notifications.dao;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;

public interface NotificationsDao
{
  Exception checkDB();

  void migrateDB() throws TapisException;
}
