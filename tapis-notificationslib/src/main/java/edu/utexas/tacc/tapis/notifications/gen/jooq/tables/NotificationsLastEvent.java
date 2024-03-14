/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.notifications.gen.jooq.tables;


import edu.utexas.tacc.tapis.notifications.gen.jooq.Keys;
import edu.utexas.tacc.tapis.notifications.gen.jooq.TapisNtf;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.records.NotificationsLastEventRecord;

import java.util.UUID;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function2;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row2;
import org.jooq.Schema;
import org.jooq.SelectField;
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
public class NotificationsLastEvent extends TableImpl<NotificationsLastEventRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>tapis_ntf.notifications_last_event</code>
     */
    public static final NotificationsLastEvent NOTIFICATIONS_LAST_EVENT = new NotificationsLastEvent();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<NotificationsLastEventRecord> getRecordType() {
        return NotificationsLastEventRecord.class;
    }

    /**
     * The column <code>tapis_ntf.notifications_last_event.bucket_number</code>.
     */
    public final TableField<NotificationsLastEventRecord, Integer> BUCKET_NUMBER = createField(DSL.name("bucket_number"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>tapis_ntf.notifications_last_event.event_uuid</code>.
     */
    public final TableField<NotificationsLastEventRecord, UUID> EVENT_UUID = createField(DSL.name("event_uuid"), SQLDataType.UUID.nullable(false), this, "");

    private NotificationsLastEvent(Name alias, Table<NotificationsLastEventRecord> aliased) {
        this(alias, aliased, null);
    }

    private NotificationsLastEvent(Name alias, Table<NotificationsLastEventRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>tapis_ntf.notifications_last_event</code> table
     * reference
     */
    public NotificationsLastEvent(String alias) {
        this(DSL.name(alias), NOTIFICATIONS_LAST_EVENT);
    }

    /**
     * Create an aliased <code>tapis_ntf.notifications_last_event</code> table
     * reference
     */
    public NotificationsLastEvent(Name alias) {
        this(alias, NOTIFICATIONS_LAST_EVENT);
    }

    /**
     * Create a <code>tapis_ntf.notifications_last_event</code> table reference
     */
    public NotificationsLastEvent() {
        this(DSL.name("notifications_last_event"), null);
    }

    public <O extends Record> NotificationsLastEvent(Table<O> child, ForeignKey<O, NotificationsLastEventRecord> key) {
        super(child, key, NOTIFICATIONS_LAST_EVENT);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : TapisNtf.TAPIS_NTF;
    }

    @Override
    public UniqueKey<NotificationsLastEventRecord> getPrimaryKey() {
        return Keys.NOTIFICATIONS_LAST_EVENT_PKEY;
    }

    @Override
    public NotificationsLastEvent as(String alias) {
        return new NotificationsLastEvent(DSL.name(alias), this);
    }

    @Override
    public NotificationsLastEvent as(Name alias) {
        return new NotificationsLastEvent(alias, this);
    }

    @Override
    public NotificationsLastEvent as(Table<?> alias) {
        return new NotificationsLastEvent(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public NotificationsLastEvent rename(String name) {
        return new NotificationsLastEvent(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public NotificationsLastEvent rename(Name name) {
        return new NotificationsLastEvent(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public NotificationsLastEvent rename(Table<?> name) {
        return new NotificationsLastEvent(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, UUID> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function2<? super Integer, ? super UUID, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function2<? super Integer, ? super UUID, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
