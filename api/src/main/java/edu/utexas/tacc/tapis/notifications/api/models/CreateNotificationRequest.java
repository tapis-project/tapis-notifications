package edu.utexas.tacc.tapis.notifications.api.models;

import javax.validation.constraints.NotBlank;

public class CreateNotificationRequest {

    @NotBlank
    private String body;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
