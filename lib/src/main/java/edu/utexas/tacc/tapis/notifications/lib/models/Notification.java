package edu.utexas.tacc.tapis.notifications.lib.models;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Notification {


    private String specversion;
    private String id;
    private String tenantId;
    private Instant time;
    private String source;
    private Object data;
    private String subject;
    private String type;
    private MediaType datacontenttype;
    private Map<String, Object> dataschema;
    private String topicname;



    public Notification(){}


    private Notification(Builder builder) {
        this.tenantId = builder.tenantId;
        this.data = builder.data;  // Object conforming to json schema of topic/type
        this.source = builder.source; // tapis.files.systems.{systemId}
        this.type = builder.type;  // tapis.files.transfers.progress, tapis.files.object.delete, tapis.files.object.create
        this.subject = builder.subject; // UUID of file transfer, systemId/path/to/file.txt
        this.time = Instant.now();
        this.id = builder.id;
        this.topicname =  builder.topicname;
        this.specversion = "1.0.1";
    }

    public static class Builder {
        private String topicname;
        private String tenantId;
        private String source;
        private Object data;
        private String type;
        private String subject;
        private String id;

        private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        public Builder setTopicName(String topicname) {
            this.topicname = topicname;
            return this;
        }

        public Builder setTenantId(String tenant) {
            this.tenantId = tenant;
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

        public Builder setId(String id) {
            this.id = id;
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
    public String getTopicname() {
        return topicname;
    }

    public void setTopicname(String topicname) {
        this.topicname = topicname;
    }

    public void setSpecversion(String specversion) {
        this.specversion = specversion;
    }

    @NotEmpty
    public String getTopicName() {
        return topicname;
    }

    public void setTopicName(String topicname) {
        this.topicname = topicname;
    }



    public MediaType getDatacontenttype() {
        return datacontenttype;
    }

    public void setDatacontenttype(MediaType datacontenttype) {
        this.datacontenttype = datacontenttype;
    }

    public Map<String, Object> getDataschema() {
        return dataschema;
    }

    public void setDataschema(Map<String, Object> dataschema) {
        this.dataschema = dataschema;
    }

    @NotEmpty
    public String getSpecversion() {
        return specversion;
    }

    @NotNull
    public String getId() {
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
    public String getTenantId() {
        return tenantId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setTenantId(String tenant) {
        this.tenantId = tenant;
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
            "tenant='" + tenantId + '\'' +
            ", subject='" + subject + '\'' +
            ", type='" + type + '\'' +
            '}';
    }
}
