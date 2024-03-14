/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.notifications.gen.jooq.tables;


import com.google.gson.JsonElement;

import edu.utexas.tacc.tapis.notifications.dao.JSONBToJsonElementBinding;
import edu.utexas.tacc.tapis.notifications.gen.jooq.Keys;
import edu.utexas.tacc.tapis.notifications.gen.jooq.TapisNtf;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.records.NotificationsTestsRecord;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function10;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row10;
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
public class NotificationsTests extends TableImpl<NotificationsTestsRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>tapis_ntf.notifications_tests</code>
     */
    public static final NotificationsTests NOTIFICATIONS_TESTS = new NotificationsTests();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<NotificationsTestsRecord> getRecordType() {
        return NotificationsTestsRecord.class;
    }

    /**
     * The column <code>tapis_ntf.notifications_tests.seq_id</code>.
     */
    public final TableField<NotificationsTestsRecord, Integer> SEQ_ID = createField(DSL.name("seq_id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>tapis_ntf.notifications_tests.subscr_seq_id</code>.
     */
    public final TableField<NotificationsTestsRecord, Integer> SUBSCR_SEQ_ID = createField(DSL.name("subscr_seq_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>tapis_ntf.notifications_tests.tenant</code>.
     */
    public final TableField<NotificationsTestsRecord, String> TENANT = createField(DSL.name("tenant"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>tapis_ntf.notifications_tests.owner</code>.
     */
    public final TableField<NotificationsTestsRecord, String> OWNER = createField(DSL.name("owner"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>tapis_ntf.notifications_tests.subscr_name</code>.
     */
    public final TableField<NotificationsTestsRecord, String> SUBSCR_NAME = createField(DSL.name("subscr_name"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>tapis_ntf.notifications_tests.notification_count</code>.
     */
    public final TableField<NotificationsTestsRecord, Integer> NOTIFICATION_COUNT = createField(DSL.name("notification_count"), SQLDataType.INTEGER.nullable(false).defaultValue(DSL.field("0", SQLDataType.INTEGER)), this, "");

    /**
     * The column <code>tapis_ntf.notifications_tests.notifications</code>.
     */
    public final TableField<NotificationsTestsRecord, JsonElement> NOTIFICATIONS = createField(DSL.name("notifications"), SQLDataType.JSONB.nullable(false), this, "", new JSONBToJsonElementBinding());

    /**
     * The column <code>tapis_ntf.notifications_tests.created</code>.
     */
    public final TableField<NotificationsTestsRecord, LocalDateTime> CREATED = createField(DSL.name("created"), SQLDataType.LOCALDATETIME(6).nullable(false).defaultValue(DSL.field("timezone('utc'::text, now())", SQLDataType.LOCALDATETIME)), this, "");

    /**
     * The column <code>tapis_ntf.notifications_tests.updated</code>.
     */
    public final TableField<NotificationsTestsRecord, LocalDateTime> UPDATED = createField(DSL.name("updated"), SQLDataType.LOCALDATETIME(6).nullable(false).defaultValue(DSL.field("timezone('utc'::text, now())", SQLDataType.LOCALDATETIME)), this, "");

    /**
     * The column <code>tapis_ntf.notifications_tests.start_count</code>.
     */
    public final TableField<NotificationsTestsRecord, Integer> START_COUNT = createField(DSL.name("start_count"), SQLDataType.INTEGER.nullable(false).defaultValue(DSL.field("0", SQLDataType.INTEGER)), this, "");

    private NotificationsTests(Name alias, Table<NotificationsTestsRecord> aliased) {
        this(alias, aliased, null);
    }

    private NotificationsTests(Name alias, Table<NotificationsTestsRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>tapis_ntf.notifications_tests</code> table
     * reference
     */
    public NotificationsTests(String alias) {
        this(DSL.name(alias), NOTIFICATIONS_TESTS);
    }

    /**
     * Create an aliased <code>tapis_ntf.notifications_tests</code> table
     * reference
     */
    public NotificationsTests(Name alias) {
        this(alias, NOTIFICATIONS_TESTS);
    }

    /**
     * Create a <code>tapis_ntf.notifications_tests</code> table reference
     */
    public NotificationsTests() {
        this(DSL.name("notifications_tests"), null);
    }

    public <O extends Record> NotificationsTests(Table<O> child, ForeignKey<O, NotificationsTestsRecord> key) {
        super(child, key, NOTIFICATIONS_TESTS);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : TapisNtf.TAPIS_NTF;
    }

    @Override
    public Identity<NotificationsTestsRecord, Integer> getIdentity() {
        return (Identity<NotificationsTestsRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<NotificationsTestsRecord> getPrimaryKey() {
        return Keys.NOTIFICATIONS_TESTS_PKEY;
    }

    @Override
    public List<UniqueKey<NotificationsTestsRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.NOTIFICATIONS_TESTS_TENANT_OWNER_SUBSCR_NAME_KEY);
    }

    @Override
    public List<ForeignKey<NotificationsTestsRecord, ?>> getReferences() {
        return Arrays.asList(Keys.NOTIFICATIONS_TESTS__NOTIFICATIONS_TESTS_SUBSCR_SEQ_ID_FKEY);
    }

    private transient Subscriptions _subscriptions;

    /**
     * Get the implicit join path to the <code>tapis_ntf.subscriptions</code>
     * table.
     */
    public Subscriptions subscriptions() {
        if (_subscriptions == null)
            _subscriptions = new Subscriptions(this, Keys.NOTIFICATIONS_TESTS__NOTIFICATIONS_TESTS_SUBSCR_SEQ_ID_FKEY);

        return _subscriptions;
    }

    @Override
    public NotificationsTests as(String alias) {
        return new NotificationsTests(DSL.name(alias), this);
    }

    @Override
    public NotificationsTests as(Name alias) {
        return new NotificationsTests(alias, this);
    }

    @Override
    public NotificationsTests as(Table<?> alias) {
        return new NotificationsTests(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public NotificationsTests rename(String name) {
        return new NotificationsTests(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public NotificationsTests rename(Name name) {
        return new NotificationsTests(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public NotificationsTests rename(Table<?> name) {
        return new NotificationsTests(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row10 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row10<Integer, Integer, String, String, String, Integer, JsonElement, LocalDateTime, LocalDateTime, Integer> fieldsRow() {
        return (Row10) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function10<? super Integer, ? super Integer, ? super String, ? super String, ? super String, ? super Integer, ? super JsonElement, ? super LocalDateTime, ? super LocalDateTime, ? super Integer, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function10<? super Integer, ? super Integer, ? super String, ? super String, ? super String, ? super Integer, ? super JsonElement, ? super LocalDateTime, ? super LocalDateTime, ? super Integer, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
