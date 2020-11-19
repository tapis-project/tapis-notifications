package edu.utexas.tacc.tapis.notifications.api.models;

import javax.validation.constraints.Email;

public class EmailSubscriptionRequest {

    @Email(message = "A valid email is required")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


}
