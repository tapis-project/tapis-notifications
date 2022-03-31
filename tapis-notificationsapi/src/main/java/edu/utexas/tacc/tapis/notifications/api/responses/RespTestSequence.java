package edu.utexas.tacc.tapis.notifications.api.responses;

import edu.utexas.tacc.tapis.notifications.model.TestSequence;
import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;

public final class RespTestSequence extends RespAbstract
{
  public TestSequence result;
  public RespTestSequence(TestSequence r) { result = r; }
}
