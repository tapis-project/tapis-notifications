package edu.utexas.tacc.tapis.notifications.api.responses;

import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.notifications.api.responses.results.TapisSubscriptionDTO;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;

import java.util.List;

public final class RespSubscription extends RespAbstract
{
  public JsonObject result;

  public RespSubscription(Subscription s, List<String> selectList)
  {
    result = new TapisSubscriptionDTO(s).getDisplayObject(selectList);
  }
}
