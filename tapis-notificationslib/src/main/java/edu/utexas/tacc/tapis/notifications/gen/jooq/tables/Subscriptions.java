/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.notifications.gen.jooq.tables;


import com.google.gson.JsonElement;

import edu.utexas.tacc.tapis.notifications.dao.JSONBToJsonElementBinding;
import edu.utexas.tacc.tapis.notifications.gen.jooq.Keys;
import edu.utexas.tacc.tapis.notifications.gen.jooq.TapisNtf;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.records.SubscriptionsRecord;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row18;
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
public class Subscriptions extends TableImpl<SubscriptionsRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>tapis_ntf.subscriptions</code>
     */
    public static final Subscriptions SUBSCRIPTIONS = new Subscriptions();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<SubscriptionsRecord> getRecordType() {
        return SubscriptionsRecord.class;
    }

    /**
     * The column <code>tapis_ntf.subscriptions.seq_id</code>. Subscription sequence id
     */
    public final TableField<SubscriptionsRecord, Integer> SEQ_ID = createField(DSL.name("seq_id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "Subscription sequence id");

    /**
     * The column <code>tapis_ntf.subscriptions.tenant</code>. Tenant name associated with the subscription
     */
    public final TableField<SubscriptionsRecord, String> TENANT = createField(DSL.name("tenant"), SQLDataType.CLOB.nullable(false), this, "Tenant name associated with the subscription");

    /**
     * The column <code>tapis_ntf.subscriptions.id</code>. Unique name for the subscription
     */
    public final TableField<SubscriptionsRecord, String> ID = createField(DSL.name("id"), SQLDataType.CLOB.nullable(false), this, "Unique name for the subscription");

    /**
     * The column <code>tapis_ntf.subscriptions.description</code>.
     */
    public final TableField<SubscriptionsRecord, String> DESCRIPTION = createField(DSL.name("description"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>tapis_ntf.subscriptions.owner</code>. User name of owner
     */
    public final TableField<SubscriptionsRecord, String> OWNER = createField(DSL.name("owner"), SQLDataType.CLOB.nullable(false), this, "User name of owner");

    /**
     * The column <code>tapis_ntf.subscriptions.enabled</code>. Indicates if subscription is currently active and available for use
     */
    public final TableField<SubscriptionsRecord, Boolean> ENABLED = createField(DSL.name("enabled"), SQLDataType.BOOLEAN.nullable(false).defaultValue(DSL.field("true", SQLDataType.BOOLEAN)), this, "Indicates if subscription is currently active and available for use");

    /**
     * The column <code>tapis_ntf.subscriptions.type_filter</code>.
     */
    public final TableField<SubscriptionsRecord, String> TYPE_FILTER = createField(DSL.name("type_filter"), SQLDataType.CLOB.nullable(false).defaultValue(DSL.field("'*.*.*'::text", SQLDataType.CLOB)), this, "");

    /**
     * The column <code>tapis_ntf.subscriptions.type_filter1</code>.
     */
    public final TableField<SubscriptionsRecord, String> TYPE_FILTER1 = createField(DSL.name("type_filter1"), SQLDataType.CLOB.nullable(false).defaultValue(DSL.field("'*'::text", SQLDataType.CLOB)), this, "");

    /**
     * The column <code>tapis_ntf.subscriptions.type_filter2</code>.
     */
    public final TableField<SubscriptionsRecord, String> TYPE_FILTER2 = createField(DSL.name("type_filter2"), SQLDataType.CLOB.nullable(false).defaultValue(DSL.field("'*'::text", SQLDataType.CLOB)), this, "");

    /**
     * The column <code>tapis_ntf.subscriptions.type_filter3</code>.
     */
    public final TableField<SubscriptionsRecord, String> TYPE_FILTER3 = createField(DSL.name("type_filter3"), SQLDataType.CLOB.nullable(false).defaultValue(DSL.field("'*'::text", SQLDataType.CLOB)), this, "");

    /**
     * The column <code>tapis_ntf.subscriptions.subject_filter</code>.
     */
    public final TableField<SubscriptionsRecord, String> SUBJECT_FILTER = createField(DSL.name("subject_filter"), SQLDataType.CLOB.nullable(false).defaultValue(DSL.field("'*'::text", SQLDataType.CLOB)), this, "");

    /**
     * The column <code>tapis_ntf.subscriptions.delivery_methods</code>.
     */
    public final TableField<SubscriptionsRecord, JsonElement> DELIVERY_METHODS = createField(DSL.name("delivery_methods"), SQLDataType.JSONB.nullable(false), this, "", new JSONBToJsonElementBinding());

    /**
     * The column <code>tapis_ntf.subscriptions.ttl</code>.
     */
    public final TableField<SubscriptionsRecord, Integer> TTL = createField(DSL.name("ttl"), SQLDataType.INTEGER.nullable(false).defaultValue(DSL.field("'-1'::integer", SQLDataType.INTEGER)), this, "");

    /**
     * The column <code>tapis_ntf.subscriptions.notes</code>.
     */
    public final TableField<SubscriptionsRecord, JsonElement> NOTES = createField(DSL.name("notes"), SQLDataType.JSONB.nullable(false), this, "", new JSONBToJsonElementBinding());

    /**
     * The column <code>tapis_ntf.subscriptions.uuid</code>.
     */
    public final TableField<SubscriptionsRecord, java.util.UUID> UUID = createField(DSL.name("uuid"), SQLDataType.UUID.nullable(false), this, "");

    /**
     * The column <code>tapis_ntf.subscriptions.expiry</code>.
     */
    public final TableField<SubscriptionsRecord, LocalDateTime> EXPIRY = createField(DSL.name("expiry"), SQLDataType.LOCALDATETIME(6), this, "");

    /**
     * The column <code>tapis_ntf.subscriptions.created</code>. UTC time for when record was created
     */
    public final TableField<SubscriptionsRecord, LocalDateTime> CREATED = createField(DSL.name("created"), SQLDataType.LOCALDATETIME(6).nullable(false).defaultValue(DSL.field("timezone('utc'::text, now())", SQLDataType.LOCALDATETIME)), this, "UTC time for when record was created");

    /**
     * The column <code>tapis_ntf.subscriptions.updated</code>. UTC time for when record was last updated
     */
    public final TableField<SubscriptionsRecord, LocalDateTime> UPDATED = createField(DSL.name("updated"), SQLDataType.LOCALDATETIME(6).nullable(false).defaultValue(DSL.field("timezone('utc'::text, now())", SQLDataType.LOCALDATETIME)), this, "UTC time for when record was last updated");

    private Subscriptions(Name alias, Table<SubscriptionsRecord> aliased) {
        this(alias, aliased, null);
    }

    private Subscriptions(Name alias, Table<SubscriptionsRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>tapis_ntf.subscriptions</code> table reference
     */
    public Subscriptions(String alias) {
        this(DSL.name(alias), SUBSCRIPTIONS);
    }

    /**
     * Create an aliased <code>tapis_ntf.subscriptions</code> table reference
     */
    public Subscriptions(Name alias) {
        this(alias, SUBSCRIPTIONS);
    }

    /**
     * Create a <code>tapis_ntf.subscriptions</code> table reference
     */
    public Subscriptions() {
        this(DSL.name("subscriptions"), null);
    }

    public <O extends Record> Subscriptions(Table<O> child, ForeignKey<O, SubscriptionsRecord> key) {
        super(child, key, SUBSCRIPTIONS);
    }

    @Override
    public Schema getSchema() {
        return TapisNtf.TAPIS_NTF;
    }

    @Override
    public Identity<SubscriptionsRecord, Integer> getIdentity() {
        return (Identity<SubscriptionsRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<SubscriptionsRecord> getPrimaryKey() {
        return Keys.SUBSCRIPTIONS_PKEY;
    }

    @Override
    public List<UniqueKey<SubscriptionsRecord>> getKeys() {
        return Arrays.<UniqueKey<SubscriptionsRecord>>asList(Keys.SUBSCRIPTIONS_PKEY, Keys.SUBSCRIPTIONS_TENANT_ID_KEY);
    }

    @Override
    public Subscriptions as(String alias) {
        return new Subscriptions(DSL.name(alias), this);
    }

    @Override
    public Subscriptions as(Name alias) {
        return new Subscriptions(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Subscriptions rename(String name) {
        return new Subscriptions(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Subscriptions rename(Name name) {
        return new Subscriptions(name, null);
    }

    // -------------------------------------------------------------------------
    // Row18 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row18<Integer, String, String, String, String, Boolean, String, String, String, String, String, JsonElement, Integer, JsonElement, java.util.UUID, LocalDateTime, LocalDateTime, LocalDateTime> fieldsRow() {
        return (Row18) super.fieldsRow();
    }
}
