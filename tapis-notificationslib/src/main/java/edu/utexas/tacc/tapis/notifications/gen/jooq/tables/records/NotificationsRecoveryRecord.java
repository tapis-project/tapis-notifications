/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.notifications.gen.jooq.tables.records;


import com.google.gson.JsonElement;

import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.NotificationsRecovery;

import java.time.LocalDateTime;
import java.util.UUID;

import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Record1;
import org.jooq.Record12;
import org.jooq.Row12;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class NotificationsRecoveryRecord extends UpdatableRecordImpl<NotificationsRecoveryRecord> implements Record12<Integer, Integer, String, Integer, UUID, JSONB, JsonElement, Integer, LocalDateTime, LocalDateTime, LocalDateTime, LocalDateTime> {

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
     * Setter for <code>tapis_ntf.notifications_recovery.tenant</code>.
     */
    public void setTenant(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.tenant</code>.
     */
    public String getTenant() {
        return (String) get(2);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.bucket_number</code>.
     */
    public void setBucketNumber(Integer value) {
        set(3, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.bucket_number</code>.
     */
    public Integer getBucketNumber() {
        return (Integer) get(3);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.event_uuid</code>.
     */
    public void setEventUuid(UUID value) {
        set(4, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.event_uuid</code>.
     */
    public UUID getEventUuid() {
        return (UUID) get(4);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.event</code>.
     */
    public void setEvent(JSONB value) {
        set(5, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.event</code>.
     */
    public JSONB getEvent() {
        return (JSONB) get(5);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.delivery_method</code>.
     */
    public void setDeliveryMethod(JsonElement value) {
        set(6, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.delivery_method</code>.
     */
    public JsonElement getDeliveryMethod() {
        return (JsonElement) get(6);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.recovery_attempt</code>.
     */
    public void setRecoveryAttempt(Integer value) {
        set(7, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.recovery_attempt</code>.
     */
    public Integer getRecoveryAttempt() {
        return (Integer) get(7);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.last_attempt</code>.
     */
    public void setLastAttempt(LocalDateTime value) {
        set(8, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.last_attempt</code>.
     */
    public LocalDateTime getLastAttempt() {
        return (LocalDateTime) get(8);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.expiry</code>.
     */
    public void setExpiry(LocalDateTime value) {
        set(9, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.expiry</code>.
     */
    public LocalDateTime getExpiry() {
        return (LocalDateTime) get(9);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.created</code>.
     */
    public void setCreated(LocalDateTime value) {
        set(10, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.created</code>.
     */
    public LocalDateTime getCreated() {
        return (LocalDateTime) get(10);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_recovery.updated</code>.
     */
    public void setUpdated(LocalDateTime value) {
        set(11, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_recovery.updated</code>.
     */
    public LocalDateTime getUpdated() {
        return (LocalDateTime) get(11);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record12 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row12<Integer, Integer, String, Integer, UUID, JSONB, JsonElement, Integer, LocalDateTime, LocalDateTime, LocalDateTime, LocalDateTime> fieldsRow() {
        return (Row12) super.fieldsRow();
    }

    @Override
    public Row12<Integer, Integer, String, Integer, UUID, JSONB, JsonElement, Integer, LocalDateTime, LocalDateTime, LocalDateTime, LocalDateTime> valuesRow() {
        return (Row12) super.valuesRow();
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
    public Field<String> field3() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.TENANT;
    }

    @Override
    public Field<Integer> field4() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.BUCKET_NUMBER;
    }

    @Override
    public Field<UUID> field5() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.EVENT_UUID;
    }

    @Override
    public Field<JSONB> field6() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.EVENT;
    }

    @Override
    public Field<JsonElement> field7() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.DELIVERY_METHOD;
    }

    @Override
    public Field<Integer> field8() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.RECOVERY_ATTEMPT;
    }

    @Override
    public Field<LocalDateTime> field9() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.LAST_ATTEMPT;
    }

    @Override
    public Field<LocalDateTime> field10() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.EXPIRY;
    }

    @Override
    public Field<LocalDateTime> field11() {
        return NotificationsRecovery.NOTIFICATIONS_RECOVERY.CREATED;
    }

    @Override
    public Field<LocalDateTime> field12() {
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
    public String component3() {
        return getTenant();
    }

    @Override
    public Integer component4() {
        return getBucketNumber();
    }

    @Override
    public UUID component5() {
        return getEventUuid();
    }

    @Override
    public JSONB component6() {
        return getEvent();
    }

    @Override
    public JsonElement component7() {
        return getDeliveryMethod();
    }

    @Override
    public Integer component8() {
        return getRecoveryAttempt();
    }

    @Override
    public LocalDateTime component9() {
        return getLastAttempt();
    }

    @Override
    public LocalDateTime component10() {
        return getExpiry();
    }

    @Override
    public LocalDateTime component11() {
        return getCreated();
    }

    @Override
    public LocalDateTime component12() {
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
    public String value3() {
        return getTenant();
    }

    @Override
    public Integer value4() {
        return getBucketNumber();
    }

    @Override
    public UUID value5() {
        return getEventUuid();
    }

    @Override
    public JSONB value6() {
        return getEvent();
    }

    @Override
    public JsonElement value7() {
        return getDeliveryMethod();
    }

    @Override
    public Integer value8() {
        return getRecoveryAttempt();
    }

    @Override
    public LocalDateTime value9() {
        return getLastAttempt();
    }

    @Override
    public LocalDateTime value10() {
        return getExpiry();
    }

    @Override
    public LocalDateTime value11() {
        return getCreated();
    }

    @Override
    public LocalDateTime value12() {
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
    public NotificationsRecoveryRecord value3(String value) {
        setTenant(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value4(Integer value) {
        setBucketNumber(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value5(UUID value) {
        setEventUuid(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value6(JSONB value) {
        setEvent(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value7(JsonElement value) {
        setDeliveryMethod(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value8(Integer value) {
        setRecoveryAttempt(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value9(LocalDateTime value) {
        setLastAttempt(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value10(LocalDateTime value) {
        setExpiry(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value11(LocalDateTime value) {
        setCreated(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord value12(LocalDateTime value) {
        setUpdated(value);
        return this;
    }

    @Override
    public NotificationsRecoveryRecord values(Integer value1, Integer value2, String value3, Integer value4, UUID value5, JSONB value6, JsonElement value7, Integer value8, LocalDateTime value9, LocalDateTime value10, LocalDateTime value11, LocalDateTime value12) {
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
    public NotificationsRecoveryRecord(Integer seqId, Integer subscrSeqId, String tenant, Integer bucketNumber, UUID eventUuid, JSONB event, JsonElement deliveryMethod, Integer recoveryAttempt, LocalDateTime lastAttempt, LocalDateTime expiry, LocalDateTime created, LocalDateTime updated) {
        super(NotificationsRecovery.NOTIFICATIONS_RECOVERY);

        setSeqId(seqId);
        setSubscrSeqId(subscrSeqId);
        setTenant(tenant);
        setBucketNumber(bucketNumber);
        setEventUuid(eventUuid);
        setEvent(event);
        setDeliveryMethod(deliveryMethod);
        setRecoveryAttempt(recoveryAttempt);
        setLastAttempt(lastAttempt);
        setExpiry(expiry);
        setCreated(created);
        setUpdated(updated);
    }
}
