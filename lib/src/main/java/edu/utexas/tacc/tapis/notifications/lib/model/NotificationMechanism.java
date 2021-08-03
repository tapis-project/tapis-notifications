package edu.utexas.tacc.tapis.notifications.lib.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;

import javax.validation.ValidationException;
import java.time.Instant;
import java.util.UUID;

public class NotificationMechanism {

    private NotificationMechanismEnum mechanism;
    private String target;
    private int subscriptionId;
    private Instant created;
    private UUID uuid;
    private String tenantId;

    public NotificationMechanism(){}

    public NotificationMechanism(NotificationMechanismEnum mechanism, String target) throws ValidationException {
        this.mechanism = mechanism;
        this.target = target;
        validate();
    }

    private void validate() throws ValidationException {
        EmailValidator emailValidator = EmailValidator.getInstance();
        UrlValidator urlValidator = UrlValidator.getInstance();
        if ((this.mechanism == NotificationMechanismEnum.EMAIL) && (!(emailValidator.isValid(this.target)))) {
            throw new ValidationException("Invalid email");
        }

        if ((this.mechanism == NotificationMechanismEnum.WEBHOOK) && (!(urlValidator.isValid(this.target)))) {
            throw new ValidationException("Invalid URL");
        }
    }

    public NotificationMechanismEnum getMechanism() {
        return mechanism;
    }

    public String getTarget() {
        return target;
    }

    @JsonIgnore
    public int getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(int subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }


}
