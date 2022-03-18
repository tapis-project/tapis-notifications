package edu.utexas.tacc.tapis.notifications.model;

import com.google.gson.JsonElement;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/*
 * Results from a test sequence of notifications.
 * Includes the subscription and all events received during the test.
 * Most attributes for this class are immutable.
 * Only receivedEvents may be modified by calling addReceivedEvent.
 */
public final class TestSequence
{
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************
  public static final JsonElement EMPTY_EVENTS = TapisGsonUtils.getGson().fromJson("[]", JsonElement.class);

  // ************************************************************************
  // *********************** Enums ******************************************
  // ************************************************************************

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************
  private final int seqId;
  private final String tenant;
  private final String owner;
  private final String subscriptionId;
  private final List<Event> receivedEvents;
  private final Instant created; // UTC time for when record was created
  private final Instant updated; // UTC time for when record was last updated

  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

  /**
   * Constructor taking all attributes.
   * If receivedEvents is null an empty list will be created.
   */
  public TestSequence(int seqId1, String tenant1, String owner1, String subscriptionId1, List<Event> reList1,
                      Instant created1, Instant updated1)
  {
    seqId = seqId1;
    tenant = tenant1;
    owner = owner1;
    subscriptionId = subscriptionId1;
    receivedEvents = (reList1 == null) ? new ArrayList<>() : new ArrayList<>(reList1);
    created = created1;
    updated = updated1;
  }

  // ************************************************************************
  // *********************** Public methods *********************************
  // ************************************************************************

  public void addReceivedEvent(Event e)
  {
    receivedEvents.add(e);
  }

  // ************************************************************************
  // *********************** Accessors **************************************
  // ************************************************************************

  public int getSeqId() { return seqId; }
  public String getTenant() { return tenant; }
  public String getOwner() { return owner; }
  public String getSubscriptionId() { return subscriptionId; }
  public List<Event> getReceivedEvents() { return new ArrayList<>(receivedEvents); }

  @Schema(type = "string")
  public Instant getCreated() { return created; }

  @Schema(type = "string")
  public Instant getUpdated() { return updated; }

  // ************************************************************************
  // ******************** Private methods ***********************************
  // ************************************************************************
}
