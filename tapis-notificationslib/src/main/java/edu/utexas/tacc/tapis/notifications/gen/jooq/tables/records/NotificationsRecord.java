/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.notifications.gen.jooq.tables.records;


import com.google.gson.JsonElement;

import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.Notifications;

import java.time.LocalDateTime;
import java.util.UUID;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record9;
import org.jooq.Row9;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class NotificationsRecord extends UpdatableRecordImpl<NotificationsRecord> implements Record9<Integer, Integer, String, Integer, UUID, JsonElement, JsonElement, LocalDateTime, LocalDateTime> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>tapis_ntf.notifications.seq_id</code>.
     */
    public void setSeqId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications.seq_id</code>.
     */
    public Integer getSeqId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>tapis_ntf.notifications.subscr_seq_id</code>.
     */
    public void setSubscrSeqId(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications.subscr_seq_id</code>.
     */
    public Integer getSubscrSeqId() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>tapis_ntf.notifications.tenant</code>.
     */
    public void setTenant(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications.tenant</code>.
     */
    public String getTenant() {
        return (String) get(2);
    }

    /**
     * Setter for <code>tapis_ntf.notifications.bucket_number</code>.
     */
    public void setBucketNumber(Integer value) {
        set(3, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications.bucket_number</code>.
     */
    public Integer getBucketNumber() {
        return (Integer) get(3);
    }

    /**
     * Setter for <code>tapis_ntf.notifications.event_uuid</code>.
     */
    public void setEventUuid(UUID value) {
        set(4, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications.event_uuid</code>.
     */
    public UUID getEventUuid() {
        return (UUID) get(4);
    }

    /**
     * Setter for <code>tapis_ntf.notifications.event</code>.
     */
    public void setEvent(JsonElement value) {
        set(5, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications.event</code>.
     */
    public JsonElement getEvent() {
        return (JsonElement) get(5);
    }

    /**
     * Setter for <code>tapis_ntf.notifications.delivery_method</code>.
     */
    public void setDeliveryMethod(JsonElement value) {
        set(6, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications.delivery_method</code>.
     */
    public JsonElement getDeliveryMethod() {
        return (JsonElement) get(6);
    }

    /**
     * Setter for <code>tapis_ntf.notifications.created</code>.
     */
    public void setCreated(LocalDateTime value) {
        set(7, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications.created</code>.
     */
    public LocalDateTime getCreated() {
        return (LocalDateTime) get(7);
    }

    /**
     * Setter for <code>tapis_ntf.notifications.updated</code>.
     */
    public void setUpdated(LocalDateTime value) {
        set(8, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications.updated</code>.
     */
    public LocalDateTime getUpdated() {
        return (LocalDateTime) get(8);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record9 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row9<Integer, Integer, String, Integer, UUID, JsonElement, JsonElement, LocalDateTime, LocalDateTime> fieldsRow() {
        return (Row9) super.fieldsRow();
    }

    @Override
    public Row9<Integer, Integer, String, Integer, UUID, JsonElement, JsonElement, LocalDateTime, LocalDateTime> valuesRow() {
        return (Row9) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return Notifications.NOTIFICATIONS.SEQ_ID;
    }

    @Override
    public Field<Integer> field2() {
        return Notifications.NOTIFICATIONS.SUBSCR_SEQ_ID;
    }

    @Override
    public Field<String> field3() {
        return Notifications.NOTIFICATIONS.TENANT;
    }

    @Override
    public Field<Integer> field4() {
        return Notifications.NOTIFICATIONS.BUCKET_NUMBER;
    }

    @Override
    public Field<UUID> field5() {
        return Notifications.NOTIFICATIONS.EVENT_UUID;
    }

    @Override
    public Field<JsonElement> field6() {
        return Notifications.NOTIFICATIONS.EVENT;
    }

    @Override
    public Field<JsonElement> field7() {
        return Notifications.NOTIFICATIONS.DELIVERY_METHOD;
    }

    @Override
    public Field<LocalDateTime> field8() {
        return Notifications.NOTIFICATIONS.CREATED;
    }

    @Override
    public Field<LocalDateTime> field9() {
        return Notifications.NOTIFICATIONS.UPDATED;
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
    public JsonElement component6() {
        return getEvent();
    }

    @Override
    public JsonElement component7() {
        return getDeliveryMethod();
    }

    @Override
    public LocalDateTime component8() {
        return getCreated();
    }

    @Override
    public LocalDateTime component9() {
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
    public JsonElement value6() {
        return getEvent();
    }

    @Override
    public JsonElement value7() {
        return getDeliveryMethod();
    }

    @Override
    public LocalDateTime value8() {
        return getCreated();
    }

    @Override
    public LocalDateTime value9() {
        return getUpdated();
    }

    @Override
    public NotificationsRecord value1(Integer value) {
        setSeqId(value);
        return this;
    }

    @Override
    public NotificationsRecord value2(Integer value) {
        setSubscrSeqId(value);
        return this;
    }

    @Override
    public NotificationsRecord value3(String value) {
        setTenant(value);
        return this;
    }

    @Override
    public NotificationsRecord value4(Integer value) {
        setBucketNumber(value);
        return this;
    }

    @Override
    public NotificationsRecord value5(UUID value) {
        setEventUuid(value);
        return this;
    }

    @Override
    public NotificationsRecord value6(JsonElement value) {
        setEvent(value);
        return this;
    }

    @Override
    public NotificationsRecord value7(JsonElement value) {
        setDeliveryMethod(value);
        return this;
    }

    @Override
    public NotificationsRecord value8(LocalDateTime value) {
        setCreated(value);
        return this;
    }

    @Override
    public NotificationsRecord value9(LocalDateTime value) {
        setUpdated(value);
        return this;
    }

    @Override
    public NotificationsRecord values(Integer value1, Integer value2, String value3, Integer value4, UUID value5, JsonElement value6, JsonElement value7, LocalDateTime value8, LocalDateTime value9) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached NotificationsRecord
     */
    public NotificationsRecord() {
        super(Notifications.NOTIFICATIONS);
    }

    /**
     * Create a detached, initialised NotificationsRecord
     */
    public NotificationsRecord(Integer seqId, Integer subscrSeqId, String tenant, Integer bucketNumber, UUID eventUuid, JsonElement event, JsonElement deliveryMethod, LocalDateTime created, LocalDateTime updated) {
        super(Notifications.NOTIFICATIONS);

        setSeqId(seqId);
        setSubscrSeqId(subscrSeqId);
        setTenant(tenant);
        setBucketNumber(bucketNumber);
        setEventUuid(eventUuid);
        setEvent(event);
        setDeliveryMethod(deliveryMethod);
        setCreated(created);
        setUpdated(updated);
    }
}
