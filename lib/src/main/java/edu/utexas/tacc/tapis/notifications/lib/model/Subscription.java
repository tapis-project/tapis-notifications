package edu.utexas.tacc.tapis.notifications.lib.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Subscription {

    private int id;
    private UUID uuid;
    private String tenantId;
    private Map<String, Object> filters;
    private Instant created;
    private List<NotificationMechanism> mechanisms = new ArrayList<>();
    private int topicId;


    @JsonIgnore
    public int getTopicId() {
        return topicId;
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @JsonIgnore
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    @Schema(type="string", format = "date-time")
    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }
    public void setCreated(String created) {
        this.created = Instant.parse(created);
    }


    public List<NotificationMechanism> getMechanisms() {
        return mechanisms;
    }

    public void setMechanisms(List<NotificationMechanism> mechanisms) {
        this.mechanisms = mechanisms;
    }

    public void addMechanism(NotificationMechanism mechanism) {
        this.mechanisms.add(mechanism);
    }


}
