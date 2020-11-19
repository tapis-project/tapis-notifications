package edu.utexas.tacc.tapis.notifications.api.models;

import javax.validation.constraints.NotBlank;

public class QueueSubscriptionRequest {

    @NotBlank
    private String queueName;

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }
}
