package edu.utexas.tacc.tapis.notifications.api.model;

import edu.utexas.tacc.tapis.notifications.lib.model.NotificationMechanism;

import javax.validation.constraints.NotEmpty;
import java.util.List;

public class CreateSubscriptionRequest {

    private String filter;

    @NotEmpty
    private List<NotificationMechanism> notificationMechanisms;

    @NotEmpty
    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public List<NotificationMechanism> getNotificationMechanisms() {
        return notificationMechanisms;
    }

    public void setNotificationMechanisms(List<NotificationMechanism> notificationMechanisms) {
        this.notificationMechanisms = notificationMechanisms;
    }
}
