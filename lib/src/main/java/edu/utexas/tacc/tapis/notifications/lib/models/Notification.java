package edu.utexas.tacc.tapis.notifications.lib.models;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Notification {


    private String specversion;
    private UUID id;
    private String tenant;
    private Instant time;
    private String source;
    private Object data;
    private String subject;
    private String type;

    public Notification(){}


    private Notification(Builder builder) {
        this.tenant = builder.tenant;
        this.data = builder.data;  // Object conforming to json schema of topic/type
        this.source = builder.source; // filesService
        this.type = builder.type;  // tapis.files.transfers.progress, tapis.files.object.delete, tapis.files.object.create
        this.subject = builder.subject; // UUID of file transfer, systemId/path/to/file.txt
        this.time = Instant.now();
        this.id = UUID.randomUUID();
        this.specversion = "1.0";
    }

    public static class Builder {
        private String tenant;
        private String source;
        private Object data;
        private String type;
        private String subject;

        private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        public Builder setTenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        public Builder setSource(String source) {
            this.source = source;
            return this;
        }

        public Builder setData(Object data) {
            this.data = data;
            return this;
        }

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setSubject(String subject) {
            this.subject = subject;
            return this;
        }


        public Notification build() {

            Notification notification = new Notification(this);
            Set<ConstraintViolation<Notification>> violations = validator.validate(notification);

            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(
                    new HashSet<>(violations));
            }

            return notification;
        }
    }

    @NotEmpty
    public String getSpecversion() {
        return specversion;
    }

    @NotNull
    public UUID getId() {
        return id;
    }

    @NotEmpty
    public String getSubject() {
        return subject;
    }

    @NotNull
    public Instant getTime() {
        return time;
    }

    @NotEmpty
    public String getSource() {
        return source;
    }

    @NotNull
    public Object getData() {
        return data;
    }

    @NotEmpty
    public String getType() {
        return type;
    }

    @NotEmpty
    public String getTenant() {
        return tenant;
    }


    public void setId(UUID id) {
        this.id = id;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Notification{" +
            "tenant='" + tenant + '\'' +
            ", created=" + time +
            ", creator='" + source + '\'' +
            ", subject='" + subject + '\'' +
            ", type='" + type + '\'' +
            '}';
    }
}
