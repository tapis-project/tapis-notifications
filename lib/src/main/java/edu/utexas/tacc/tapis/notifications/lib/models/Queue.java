package edu.utexas.tacc.tapis.notifications.lib.models;

import javax.validation.constraints.NotBlank;
import java.time.Instant;

public class Queue {

    private String name;
    private Instant created;
    private String tenantId;
    private String owner;

    @NotBlank
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    @NotBlank
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @NotBlank
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
