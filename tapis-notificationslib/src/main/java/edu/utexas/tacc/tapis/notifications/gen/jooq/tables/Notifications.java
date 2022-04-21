/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.notifications.gen.jooq.tables;


import com.google.gson.JsonElement;

import edu.utexas.tacc.tapis.notifications.dao.JSONBToJsonElementBinding;
import edu.utexas.tacc.tapis.notifications.gen.jooq.Keys;
import edu.utexas.tacc.tapis.notifications.gen.jooq.TapisNtf;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.records.NotificationsRecord;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row10;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Notifications extends TableImpl<NotificationsRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>tapis_ntf.notifications</code>
     */
    public static final Notifications NOTIFICATIONS = new Notifications();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<NotificationsRecord> getRecordType() {
        return NotificationsRecord.class;
    }

    /**
     * The column <code>tapis_ntf.notifications.seq_id</code>.
     */
    public final TableField<NotificationsRecord, Integer> SEQ_ID = createField(DSL.name("seq_id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>tapis_ntf.notifications.subscr_seq_id</code>.
     */
    public final TableField<NotificationsRecord, Integer> SUBSCR_SEQ_ID = createField(DSL.name("subscr_seq_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>tapis_ntf.notifications.uuid</code>.
     */
    public final TableField<NotificationsRecord, java.util.UUID> UUID = createField(DSL.name("uuid"), SQLDataType.UUID.nullable(false), this, "");

    /**
     * The column <code>tapis_ntf.notifications.tenant</code>.
     */
    public final TableField<NotificationsRecord, String> TENANT = createField(DSL.name("tenant"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>tapis_ntf.notifications.subscr_id</code>.
     */
    public final TableField<NotificationsRecord, String> SUBSCR_ID = createField(DSL.name("subscr_id"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>tapis_ntf.notifications.delivery_method</code>.
     */
    public final TableField<NotificationsRecord, JsonElement> DELIVERY_METHOD = createField(DSL.name("delivery_method"), SQLDataType.JSONB.nullable(false), this, "", new JSONBToJsonElementBinding());

    /**
     * The column <code>tapis_ntf.notifications.event_uuid</code>.
     */
    public final TableField<NotificationsRecord, java.util.UUID> EVENT_UUID = createField(DSL.name("event_uuid"), SQLDataType.UUID.nullable(false), this, "");

    /**
     * The column <code>tapis_ntf.notifications.event</code>.
     */
    public final TableField<NotificationsRecord, JsonElement> EVENT = createField(DSL.name("event"), SQLDataType.JSONB.nullable(false), this, "", new JSONBToJsonElementBinding());

    /**
     * The column <code>tapis_ntf.notifications.bucket_number</code>.
     */
    public final TableField<NotificationsRecord, Integer> BUCKET_NUMBER = createField(DSL.name("bucket_number"), SQLDataType.INTEGER.nullable(false).defaultValue(DSL.field("0", SQLDataType.INTEGER)), this, "");

    /**
     * The column <code>tapis_ntf.notifications.created</code>.
     */
    public final TableField<NotificationsRecord, LocalDateTime> CREATED = createField(DSL.name("created"), SQLDataType.LOCALDATETIME(6).nullable(false).defaultValue(DSL.field("timezone('utc'::text, now())", SQLDataType.LOCALDATETIME)), this, "");

    private Notifications(Name alias, Table<NotificationsRecord> aliased) {
        this(alias, aliased, null);
    }

    private Notifications(Name alias, Table<NotificationsRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>tapis_ntf.notifications</code> table reference
     */
    public Notifications(String alias) {
        this(DSL.name(alias), NOTIFICATIONS);
    }

    /**
     * Create an aliased <code>tapis_ntf.notifications</code> table reference
     */
    public Notifications(Name alias) {
        this(alias, NOTIFICATIONS);
    }

    /**
     * Create a <code>tapis_ntf.notifications</code> table reference
     */
    public Notifications() {
        this(DSL.name("notifications"), null);
    }

    public <O extends Record> Notifications(Table<O> child, ForeignKey<O, NotificationsRecord> key) {
        super(child, key, NOTIFICATIONS);
    }

    @Override
    public Schema getSchema() {
        return TapisNtf.TAPIS_NTF;
    }

    @Override
    public Identity<NotificationsRecord, Integer> getIdentity() {
        return (Identity<NotificationsRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<NotificationsRecord> getPrimaryKey() {
        return Keys.NOTIFICATIONS_PKEY;
    }

    @Override
    public List<UniqueKey<NotificationsRecord>> getKeys() {
        return Arrays.<UniqueKey<NotificationsRecord>>asList(Keys.NOTIFICATIONS_PKEY);
    }

    @Override
    public List<ForeignKey<NotificationsRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<NotificationsRecord, ?>>asList(Keys.NOTIFICATIONS__NOTIFICATIONS_SUBSCR_SEQ_ID_FKEY);
    }

    private transient Subscriptions _subscriptions;

    public Subscriptions subscriptions() {
        if (_subscriptions == null)
            _subscriptions = new Subscriptions(this, Keys.NOTIFICATIONS__NOTIFICATIONS_SUBSCR_SEQ_ID_FKEY);

        return _subscriptions;
    }

    @Override
    public Notifications as(String alias) {
        return new Notifications(DSL.name(alias), this);
    }

    @Override
    public Notifications as(Name alias) {
        return new Notifications(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Notifications rename(String name) {
        return new Notifications(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Notifications rename(Name name) {
        return new Notifications(name, null);
    }

    // -------------------------------------------------------------------------
    // Row10 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row10<Integer, Integer, java.util.UUID, String, String, JsonElement, java.util.UUID, JsonElement, Integer, LocalDateTime> fieldsRow() {
        return (Row10) super.fieldsRow();
    }
}
