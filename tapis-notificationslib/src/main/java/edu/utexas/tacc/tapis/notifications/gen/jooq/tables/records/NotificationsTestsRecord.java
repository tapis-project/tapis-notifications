/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.notifications.gen.jooq.tables.records;


import com.google.gson.JsonElement;

import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.NotificationsTests;

import java.time.LocalDateTime;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record9;
import org.jooq.Row9;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class NotificationsTestsRecord extends UpdatableRecordImpl<NotificationsTestsRecord> implements Record9<Integer, Integer, String, String, String, Integer, JsonElement, LocalDateTime, LocalDateTime> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>tapis_ntf.notifications_tests.seq_id</code>.
     */
    public void setSeqId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_tests.seq_id</code>.
     */
    public Integer getSeqId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_tests.subscr_seq_id</code>.
     */
    public void setSubscrSeqId(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_tests.subscr_seq_id</code>.
     */
    public Integer getSubscrSeqId() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_tests.tenant</code>.
     */
    public void setTenant(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_tests.tenant</code>.
     */
    public String getTenant() {
        return (String) get(2);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_tests.subscr_id</code>.
     */
    public void setSubscrId(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_tests.subscr_id</code>.
     */
    public String getSubscrId() {
        return (String) get(3);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_tests.owner</code>.
     */
    public void setOwner(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_tests.owner</code>.
     */
    public String getOwner() {
        return (String) get(4);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_tests.notification_count</code>.
     */
    public void setNotificationCount(Integer value) {
        set(5, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_tests.notification_count</code>.
     */
    public Integer getNotificationCount() {
        return (Integer) get(5);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_tests.notifications</code>.
     */
    public void setNotifications(JsonElement value) {
        set(6, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_tests.notifications</code>.
     */
    public JsonElement getNotifications() {
        return (JsonElement) get(6);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_tests.created</code>.
     */
    public void setCreated(LocalDateTime value) {
        set(7, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_tests.created</code>.
     */
    public LocalDateTime getCreated() {
        return (LocalDateTime) get(7);
    }

    /**
     * Setter for <code>tapis_ntf.notifications_tests.updated</code>.
     */
    public void setUpdated(LocalDateTime value) {
        set(8, value);
    }

    /**
     * Getter for <code>tapis_ntf.notifications_tests.updated</code>.
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
    public Row9<Integer, Integer, String, String, String, Integer, JsonElement, LocalDateTime, LocalDateTime> fieldsRow() {
        return (Row9) super.fieldsRow();
    }

    @Override
    public Row9<Integer, Integer, String, String, String, Integer, JsonElement, LocalDateTime, LocalDateTime> valuesRow() {
        return (Row9) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return NotificationsTests.NOTIFICATIONS_TESTS.SEQ_ID;
    }

    @Override
    public Field<Integer> field2() {
        return NotificationsTests.NOTIFICATIONS_TESTS.SUBSCR_SEQ_ID;
    }

    @Override
    public Field<String> field3() {
        return NotificationsTests.NOTIFICATIONS_TESTS.TENANT;
    }

    @Override
    public Field<String> field4() {
        return NotificationsTests.NOTIFICATIONS_TESTS.SUBSCR_ID;
    }

    @Override
    public Field<String> field5() {
        return NotificationsTests.NOTIFICATIONS_TESTS.OWNER;
    }

    @Override
    public Field<Integer> field6() {
        return NotificationsTests.NOTIFICATIONS_TESTS.NOTIFICATION_COUNT;
    }

    @Override
    public Field<JsonElement> field7() {
        return NotificationsTests.NOTIFICATIONS_TESTS.NOTIFICATIONS;
    }

    @Override
    public Field<LocalDateTime> field8() {
        return NotificationsTests.NOTIFICATIONS_TESTS.CREATED;
    }

    @Override
    public Field<LocalDateTime> field9() {
        return NotificationsTests.NOTIFICATIONS_TESTS.UPDATED;
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
    public String component4() {
        return getSubscrId();
    }

    @Override
    public String component5() {
        return getOwner();
    }

    @Override
    public Integer component6() {
        return getNotificationCount();
    }

    @Override
    public JsonElement component7() {
        return getNotifications();
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
    public String value4() {
        return getSubscrId();
    }

    @Override
    public String value5() {
        return getOwner();
    }

    @Override
    public Integer value6() {
        return getNotificationCount();
    }

    @Override
    public JsonElement value7() {
        return getNotifications();
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
    public NotificationsTestsRecord value1(Integer value) {
        setSeqId(value);
        return this;
    }

    @Override
    public NotificationsTestsRecord value2(Integer value) {
        setSubscrSeqId(value);
        return this;
    }

    @Override
    public NotificationsTestsRecord value3(String value) {
        setTenant(value);
        return this;
    }

    @Override
    public NotificationsTestsRecord value4(String value) {
        setSubscrId(value);
        return this;
    }

    @Override
    public NotificationsTestsRecord value5(String value) {
        setOwner(value);
        return this;
    }

    @Override
    public NotificationsTestsRecord value6(Integer value) {
        setNotificationCount(value);
        return this;
    }

    @Override
    public NotificationsTestsRecord value7(JsonElement value) {
        setNotifications(value);
        return this;
    }

    @Override
    public NotificationsTestsRecord value8(LocalDateTime value) {
        setCreated(value);
        return this;
    }

    @Override
    public NotificationsTestsRecord value9(LocalDateTime value) {
        setUpdated(value);
        return this;
    }

    @Override
    public NotificationsTestsRecord values(Integer value1, Integer value2, String value3, String value4, String value5, Integer value6, JsonElement value7, LocalDateTime value8, LocalDateTime value9) {
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
     * Create a detached NotificationsTestsRecord
     */
    public NotificationsTestsRecord() {
        super(NotificationsTests.NOTIFICATIONS_TESTS);
    }

    /**
     * Create a detached, initialised NotificationsTestsRecord
     */
    public NotificationsTestsRecord(Integer seqId, Integer subscrSeqId, String tenant, String subscrId, String owner, Integer notificationCount, JsonElement notifications, LocalDateTime created, LocalDateTime updated) {
        super(NotificationsTests.NOTIFICATIONS_TESTS);

        setSeqId(seqId);
        setSubscrSeqId(subscrSeqId);
        setTenant(tenant);
        setSubscrId(subscrId);
        setOwner(owner);
        setNotificationCount(notificationCount);
        setNotifications(notifications);
        setCreated(created);
        setUpdated(updated);
    }
}
