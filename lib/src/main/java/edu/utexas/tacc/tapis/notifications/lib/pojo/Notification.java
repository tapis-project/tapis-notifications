package edu.utexas.tacc.tapis.notifications.lib.pojo;

import java.time.Instant;

public class Notification implements INotification {

    private String tenant;
    private Instant created;
    private String recipient;
    private String creator;
    private String body;
    private String level;
    private String eventType; //FILE_TRANSFER_PROGRESS


    public Notification(){}

    public Notification(String tenant, String username, String creator, String body, String level) {
        this.tenant = tenant;
        this.recipient = username;
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
    public String getRecipient() {
        return recipient;
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

    public void setRecipient(String recipient) {
        this.recipient = recipient;
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

    @Override
    public String toString() {
        return "Notification{" +
            "tenant='" + tenant + '\'' +
            ", created=" + created +
            ", username='" + recipient + '\'' +
            ", creator='" + creator + '\'' +
            ", body='" + body + '\'' +
            ", level='" + level + '\'' +
            '}';
    }
}
