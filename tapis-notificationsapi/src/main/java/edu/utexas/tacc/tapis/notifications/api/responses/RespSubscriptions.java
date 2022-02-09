package edu.utexas.tacc.tapis.notifications.api.responses;

import com.google.gson.JsonArray;
import edu.utexas.tacc.tapis.notifications.api.responses.results.TapisSubscriptionDTO;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;
import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultListMetadata;

import java.util.List;

/*
  Results from a retrieval of Subscription resources.
 */
public final class RespSubscriptions extends RespAbstract
{
  public JsonArray result;

  public RespSubscriptions(List<Subscription> subscriptionList, int limit, String orderBy, int skip, String startAfter,
                           int totalCount, List<String> selectList)
  {
    result = new JsonArray();
    for (Subscription subscription : subscriptionList)
    {
      result.add(new TapisSubscriptionDTO(subscription).getDisplayObject(selectList));
    }
    ResultListMetadata meta = new ResultListMetadata();
    meta.recordCount = result.size();
    meta.recordLimit = limit;
    meta.recordsSkipped = skip;
    meta.orderBy = orderBy;
    meta.startAfter = startAfter;
    meta.totalCount = totalCount;
    metadata = meta;
  }
}