package edu.utexas.tacc.tapis.notifications.websockets;


import java.time.Instant;

public class UserNotification {

    private Object body;
    private Instant created;
    private String level;

    public UserNotification(){}

    private UserNotification(Builder builder) {
        this.body = builder.body;
        this.created = builder.created;
        this.level = builder.level;
    }

    public static class Builder {

        private Object body;
        private Instant created;
        private String level;

        public Builder setMessage(Object body) {
            this.body = body;
            return this;
        }

        public Builder setCreated(Instant created) {
            this.created = created;
            return this;
        }

        public Builder setLevel(String level) {
            this.level = level;
            return this;
        }

        public UserNotification build() {
            UserNotification notification = new UserNotification(this);
            return notification;
        }
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object message) {
        this.body = message;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    @Override
    public String toString() {
        return "UserNotification{" +
            "body='" + body + '\'' +
            ", created=" + created +
            ", level='" + level + '\'' +
            '}';
    }

}
