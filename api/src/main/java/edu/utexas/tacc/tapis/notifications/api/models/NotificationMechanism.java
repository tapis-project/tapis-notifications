package edu.utexas.tacc.tapis.notifications.api.models;

public class NotificationMechanism {

    private NotificationMechanismEnum type;
    private String target;

    public NotificationMechanismEnum getType() {
        return type;
    }

    public void setType(NotificationMechanismEnum type) {
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}
