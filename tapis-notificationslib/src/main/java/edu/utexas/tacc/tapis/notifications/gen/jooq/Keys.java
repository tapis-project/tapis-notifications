/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.notifications.gen.jooq;


import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.FlywaySchemaHistory;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.Notifications;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.NotificationsLastEvent;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.NotificationsRecovery;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.NotificationsTests;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.Subscriptions;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.records.FlywaySchemaHistoryRecord;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.records.NotificationsLastEventRecord;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.records.NotificationsRecord;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.records.NotificationsRecoveryRecord;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.records.NotificationsTestsRecord;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.records.SubscriptionsRecord;

import org.jooq.ForeignKey;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;


/**
 * A class modelling foreign key relationships and constraints of tables in 
 * tapis_ntf.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<FlywaySchemaHistoryRecord> FLYWAY_SCHEMA_HISTORY_PK = Internal.createUniqueKey(FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY, DSL.name("flyway_schema_history_pk"), new TableField[] { FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY.INSTALLED_RANK }, true);
    public static final UniqueKey<NotificationsRecord> NOTIFICATIONS_PKEY = Internal.createUniqueKey(Notifications.NOTIFICATIONS, DSL.name("notifications_pkey"), new TableField[] { Notifications.NOTIFICATIONS.SEQ_ID }, true);
    public static final UniqueKey<NotificationsLastEventRecord> NOTIFICATIONS_LAST_EVENT_PKEY = Internal.createUniqueKey(NotificationsLastEvent.NOTIFICATIONS_LAST_EVENT, DSL.name("notifications_last_event_pkey"), new TableField[] { NotificationsLastEvent.NOTIFICATIONS_LAST_EVENT.BUCKET_NUMBER }, true);
    public static final UniqueKey<NotificationsRecoveryRecord> NOTIFICATIONS_RECOVERY_PKEY = Internal.createUniqueKey(NotificationsRecovery.NOTIFICATIONS_RECOVERY, DSL.name("notifications_recovery_pkey"), new TableField[] { NotificationsRecovery.NOTIFICATIONS_RECOVERY.SEQ_ID }, true);
    public static final UniqueKey<NotificationsTestsRecord> NOTIFICATIONS_TESTS_PKEY = Internal.createUniqueKey(NotificationsTests.NOTIFICATIONS_TESTS, DSL.name("notifications_tests_pkey"), new TableField[] { NotificationsTests.NOTIFICATIONS_TESTS.SEQ_ID }, true);
    public static final UniqueKey<NotificationsTestsRecord> NOTIFICATIONS_TESTS_TENANT_OWNER_SUBSCR_NAME_KEY = Internal.createUniqueKey(NotificationsTests.NOTIFICATIONS_TESTS, DSL.name("notifications_tests_tenant_owner_subscr_name_key"), new TableField[] { NotificationsTests.NOTIFICATIONS_TESTS.TENANT, NotificationsTests.NOTIFICATIONS_TESTS.OWNER, NotificationsTests.NOTIFICATIONS_TESTS.SUBSCR_NAME }, true);
    public static final UniqueKey<SubscriptionsRecord> SUBSCRIPTIONS_PKEY = Internal.createUniqueKey(Subscriptions.SUBSCRIPTIONS, DSL.name("subscriptions_pkey"), new TableField[] { Subscriptions.SUBSCRIPTIONS.SEQ_ID }, true);
    public static final UniqueKey<SubscriptionsRecord> SUBSCRIPTIONS_TENANT_OWNER_NAME_KEY = Internal.createUniqueKey(Subscriptions.SUBSCRIPTIONS, DSL.name("subscriptions_tenant_owner_name_key"), new TableField[] { Subscriptions.SUBSCRIPTIONS.TENANT, Subscriptions.SUBSCRIPTIONS.OWNER, Subscriptions.SUBSCRIPTIONS.NAME }, true);

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------

    public static final ForeignKey<NotificationsRecord, SubscriptionsRecord> NOTIFICATIONS__NOTIFICATIONS_SUBSCR_SEQ_ID_FKEY = Internal.createForeignKey(Notifications.NOTIFICATIONS, DSL.name("notifications_subscr_seq_id_fkey"), new TableField[] { Notifications.NOTIFICATIONS.SUBSCR_SEQ_ID }, Keys.SUBSCRIPTIONS_PKEY, new TableField[] { Subscriptions.SUBSCRIPTIONS.SEQ_ID }, true);
    public static final ForeignKey<NotificationsRecoveryRecord, SubscriptionsRecord> NOTIFICATIONS_RECOVERY__NOTIFICATIONS_RECOVERY_SUBSCR_SEQ_ID_FKEY = Internal.createForeignKey(NotificationsRecovery.NOTIFICATIONS_RECOVERY, DSL.name("notifications_recovery_subscr_seq_id_fkey"), new TableField[] { NotificationsRecovery.NOTIFICATIONS_RECOVERY.SUBSCR_SEQ_ID }, Keys.SUBSCRIPTIONS_PKEY, new TableField[] { Subscriptions.SUBSCRIPTIONS.SEQ_ID }, true);
    public static final ForeignKey<NotificationsTestsRecord, SubscriptionsRecord> NOTIFICATIONS_TESTS__NOTIFICATIONS_TESTS_SUBSCR_SEQ_ID_FKEY = Internal.createForeignKey(NotificationsTests.NOTIFICATIONS_TESTS, DSL.name("notifications_tests_subscr_seq_id_fkey"), new TableField[] { NotificationsTests.NOTIFICATIONS_TESTS.SUBSCR_SEQ_ID }, Keys.SUBSCRIPTIONS_PKEY, new TableField[] { Subscriptions.SUBSCRIPTIONS.SEQ_ID }, true);
}
