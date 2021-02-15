package edu.utexas.tacc.tapis.notifications.api.models;

import javax.validation.constraints.NotBlank;

public class CreateNotificationRequest {

    @NotBlank
    private String type;

    @NotBlank
    private String data;

    @NotBlank
    private String id;

    @NotBlank
    private String subject;

    @NotBlank
    private String source;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
