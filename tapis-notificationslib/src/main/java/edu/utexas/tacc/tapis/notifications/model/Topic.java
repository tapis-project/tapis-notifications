package edu.utexas.tacc.tapis.notifications.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Topic
{
  private int id;
  private String tenantId;
  private UUID uuid;
  private String name;
  private Instant created;
  private String owner;
  private String description;
  private List<Subscription> subscriptions;

  @JsonIgnore
    public int getId() { return id; }
    public void setId(int i) { id = i; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String s) { tenantId = s; }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID u) { uuid = u; }

    public String getName() { return name; }
    public void setName(String s) { name = s; }

    @Schema(type="string", format = "date-time")
    public Instant getCreated() { return created; }
    public void setCreated(Instant c) { created = c; }
    public void setCreated(String s) { created = Instant.parse(s); }

    public String getOwner() { return owner; }
    public void setOwner(String s) { owner = s; }

    public String getDescription() { return description; }
    public void setDescription(String s) { description = s; }

    @JsonIgnore
    public List<Subscription> getSubscriptions() { return subscriptions; }
    public void setSubscriptions(List<Subscription> subs) { subscriptions = subs; }
}
