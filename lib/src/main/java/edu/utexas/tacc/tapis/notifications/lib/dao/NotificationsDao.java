package edu.utexas.tacc.tapis.notifications.lib.dao;

import edu.utexas.tacc.tapis.search.parser.ASTNode;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;

import java.util.List;
import java.util.Set;

public interface NotificationsDao
{
  Exception checkDB();

  void migrateDB() throws TapisException;
}
