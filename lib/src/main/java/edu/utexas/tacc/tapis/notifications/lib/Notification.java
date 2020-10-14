package edu.utexas.tacc.tapis.notifications.lib;

import java.time.Instant;

public class Notification implements INotification {

    private String tenant;
    private Instant created;
    private String username;
    private String creator;
    private String body;
    private String level;

    public Notification(String tenant, String username, String creator, String body, String level) {
        this.tenant = tenant;
        this.username = username;
        this.creator = creator;
        this.body = body;
        this.level = level;
        this.created = Instant.now();
    }


    @Override
    public Instant getCreated() {
        return created;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getCreator() {
        return creator;
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public String getLevel() {
        return level;
    }

    @Override
    public String getTenant() {
        return tenant;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }
}
