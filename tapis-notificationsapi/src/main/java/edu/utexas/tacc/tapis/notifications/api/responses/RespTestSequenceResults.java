package edu.utexas.tacc.tapis.notifications.api.responses;

import edu.utexas.tacc.tapis.notifications.model.TestSequenceResults;
import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;

public final class RespTestSequenceResults extends RespAbstract
{
  public TestSequenceResults result;
  public RespTestSequenceResults(TestSequenceResults r) { result = r; }
}
