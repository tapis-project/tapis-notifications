/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.notifications.gen.jooq.tables.records;


import com.google.gson.JsonElement;

import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.NotificationsRecovery;

import java.time.LocalDateTime;
import java.util.UUID;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record13;
import org.jooq.Row13;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class NotificationsRecoveryRecord extends UpdatableRecordImpl<NotificationsRecoveryRecord> implements Record13<Integer, Integer, UUID, String, String, JsonElement, UUID, JsonElement, Integer, Integer, LocalDateTime, LocalDateTime, LocalDateTime> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.seq_id</code>.
     */
    public void setSeqId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.seq_id</code>.
     */
    public Integer getSeqId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.subscr_seq_id</code>.
     */
    public void setSubscrSeqId(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.subscr_seq_id</code>.
     */
    public Integer getSubscrSeqId() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.uuid</code>.
     */
    public void setUuid(UUID value) {
        set(2, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.uuid</code>.
     */
    public UUID getUuid() {
        return (UUID) get(2);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.tenant</code>.
     */
    public void setTenant(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.tenant</code>.
     */
    public String getTenant() {
        return (String) get(3);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.subscr_name</code>.
     */
    public void setSubscrName(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.subscr_name</code>.
     */
    public String getSubscrName() {
        return (String) get(4);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.delivery_target</code>.
     */
    public void setDeliveryTarget(JsonElement value) {
        set(5, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.delivery_target</code>.
     */
    public JsonElement getDeliveryTarget() {
        return (JsonElement) get(5);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.event_uuid</code>.
     */
    public void setEventUuid(UUID value) {
        set(6, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.event_uuid</code>.
     */
    public UUID getEventUuid() {
        return (UUID) get(6);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.event</code>.
     */
    public void setEvent(JsonElement value) {
        set(7, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.event</code>.
     */
    public JsonElement getEvent() {
        return (JsonElement) get(7);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.bucket_number</code>.
     */
    public void setBucketNumber(Integer value) {
        set(8, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.bucket_number</code>.
     */
    public Integer getBucketNumber() {
        return (Integer) get(8);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.attempt_count</code>.
     */
    public void setAttemptCount(Integer value) {
        set(9, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.attempt_count</code>.
     */
    public Integer getAttemptCount() {
        return (Integer) get(9);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.last_attempt</code>.
     */
    public void setLastAttempt(LocalDateTime value) {
        set(10, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.last_attempt</code>.
     */
    public LocalDateTime getLastAttempt() {
        return (LocalDateTime) get(10);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.created</code>.
     */
    public void setCreated(LocalDateTime value) {
        set(11, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.created</code>.
     */
    public LocalDateTime getCreated() {
        return (LocalDateTime) get(11);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.updated</code>.
     */
    public void setUpdated(LocalDateTime value) {
        set(12, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.updated</code>.
     */
    public LocalDateTime getUpdated() {
        return (LocalDateTime) get(12);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record13 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row13<Integer, Integer, UUID, String, String, JsonElement, UUID, JsonElement, Integer, Integer, LocalDateTime, LocalDateTime, LocalDateTime> fieldsRow() {
        return (Row13) super.fieldsRow();
    }

    @Override
    public Row13<Integer, Integer, UUID, String, String, JsonElement, UUID, JsonElement, Integer, Integer, LocalDateTime, LocalDateTime, LocalDateTime> valuesRow() {
        return (Row13) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.SEQ_ID;
    }

    @Override
    public Field<Integer> field2() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.SUBSCR_SEQ_ID;
    }

    @Override
    public Field<UUID> field3() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.UUID;
    }

    @Override
    public Field<String> field4() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.TENANT;
    }

    @Override
    public Field<String> field5() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.SUBSCR_NAME;
    }

    @Override
    public Field<JsonElement> field6() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.DELIVERY_TARGET;
    }

    @Override
    public Field<UUID> field7() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.EVENT_UUID;
    }

    @Override
    public Field<JsonElement> field8() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.EVENT;
    }

    @Override
    public Field<Integer> field9() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.BUCKET_NUMBER;
    }

    @Override
    public Field<Integer> field10() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.ATTEMPT_COUNT;
    }

    @Override
    public Field<LocalDateTime> field11() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.LAST_ATTEMPT;
    }

    @Override
    public Field<LocalDateTime> field12() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.CREATED;
    }

    @Override
    public Field<LocalDateTime> field13() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.UPDATED;
    }

    @Override
    public Integer component1() {
        return getSeqId();
    }

    @Override
    public Integer component2() {
        return getSubscrSeqId();
    }

    @Override
    public UUID component3() {
        return getUuid();
    }

    @Override
    public String component4() {
        return getTenant();
    }

    @Override
    public String component5() {
        return getSubscrName();
    }

    @Override
    public JsonElement component6() {
        return getDeliveryTarget();
    }

    @Override
    public UUID component7() {
        return getEventUuid();
    }

    @Override
    public JsonElement component8() {
        return getEvent();
    }

    @Override
    public Integer component9() {
        return getBucketNumber();
    }

    @Override
    public Integer component10() {
        return getAttemptCount();
    }

    @Override
    public LocalDateTime component11() {
        return getLastAttempt();
    }

    @Override
    public LocalDateTime component12() {
        return getCreated();
    }

    @Override
    public LocalDateTime component13() {
        return getUpdated();
    }

    @Override
    public Integer value1() {
        return getSeqId();
    }

    @Override
    public Integer value2() {
        return getSubscrSeqId();
    }

    @Override
    public UUID value3() {
        return getUuid();
    }

    @Override
    public String value4() {
        return getTenant();
    }

    @Override
    public String value5() {
        return getSubscrName();
    }

    @Override
    public JsonElement value6() {
        return getDeliveryTarget();
    }

    @Override
    public UUID value7() {
        return getEventUuid();
    }

    @Override
    public JsonElement value8() {
        return getEvent();
    }

    @Override
    public Integer value9() {
        return getBucketNumber();
    }

    @Override
    public Integer value10() {
        return getAttemptCount();
    }

    @Override
    public LocalDateTime value11() {
        return getLastAttempt();
    }

    @Override
    public LocalDateTime value12() {
        return getCreated();
    }

    @Override
    public LocalDateTime value13() {
        return getUpdated();
    }

    @Override
    public NotificationsRecoveryRecord value1(Integer value) {
        setSeqId(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value2(Integer value) {
        setSubscrSeqId(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value3(UUID value) {
        setUuid(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value4(String value) {
        setTenant(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value5(String value) {
        setSubscrName(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value6(JsonElement value) {
        setDeliveryTarget(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value7(UUID value) {
        setEventUuid(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value8(JsonElement value) {
        setEvent(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value9(Integer value) {
        setBucketNumber(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value10(Integer value) {
        setAttemptCount(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value11(LocalDateTime value) {
        setLastAttempt(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value12(LocalDateTime value) {
        setCreated(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value13(LocalDateTime value) {
        setUpdated(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord values(Integer value1, Integer value2, UUID value3, String value4, String value5, JsonElement value6, UUID value7, JsonElement value8, Integer value9, Integer value10, LocalDateTime value11, LocalDateTime value12, LocalDateTime value13) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        value11(value11);
        value12(value12);
        value13(value13);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached NotificationsRecoveryRecord
     */
    public NotificationsRecoveryRecord() {
        super(NotificationsRecovery.NOTIFICATIONS_RECOVERY);
    }

    /**
     * Create a detached, initialised NotificationsRecoveryRecord
     */
    public NotificationsRecoveryRecord(Integer seqId, Integer subscrSeqId, UUID uuid, String tenant, String subscrName, JsonElement deliveryTarget, UUID eventUuid, JsonElement event, Integer bucketNumber, Integer attemptCount, LocalDateTime lastAttempt, LocalDateTime created, LocalDateTime updated) {
        super(NotificationsRecovery.NOTIFICATIONS_RECOVERY);

        setSeqId(seqId);
        setSubscrSeqId(subscrSeqId);
        setUuid(uuid);
        setTenant(tenant);
        setSubscrName(subscrName);
        setDeliveryTarget(deliveryTarget);
        setEventUuid(eventUuid);
        setEvent(event);
        setBucketNumber(bucketNumber);
        setAttemptCount(attemptCount);
        setLastAttempt(lastAttempt);
        setCreated(created);
        setUpdated(updated);
    }
}
