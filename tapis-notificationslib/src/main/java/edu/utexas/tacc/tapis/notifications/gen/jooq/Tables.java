/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.notifications.gen.jooq;


import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.EventSeries;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.FlywaySchemaHistory;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.Notifications;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.NotificationsLastEvent;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.NotificationsRecovery;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.NotificationsTests;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.Subscriptions;


/**
 * Convenience access to all tables in tapis_ntf.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Tables {

    /**
     * The table <code>tapis_ntf.event_series</code>.
     */
    public static final EventSeries EVENT_SERIES = EventSeries.EVENT_SERIES;

    /**
     * The table <code>tapis_ntf.flyway_schema_history</code>.
     */
    public static final FlywaySchemaHistory FLYWAY_SCHEMA_HISTORY = FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY;

    /**
     * The table <code>tapis_ntf.notifications</code>.
     */
    public static final Notifications NOTIFICATIONS = Notifications.NOTIFICATIONS;

    /**
     * The table <code>tapis_ntf.notifications_last_event</code>.
     */
    public static final NotificationsLastEvent NOTIFICATIONS_LAST_EVENT = NotificationsLastEvent.NOTIFICATIONS_LAST_EVENT;

    /**
     * The table <code>tapis_ntf.notifications_recovery</code>.
     */
    public static final NotificationsRecovery NOTIFICATIONS_RECOVERY = NotificationsRecovery.NOTIFICATIONS_RECOVERY;

    /**
     * The table <code>tapis_ntf.notifications_tests</code>.
     */
    public static final NotificationsTests NOTIFICATIONS_TESTS = NotificationsTests.NOTIFICATIONS_TESTS;

    /**
     * The table <code>tapis_ntf.subscriptions</code>.
     */
    public static final Subscriptions SUBSCRIPTIONS = Subscriptions.SUBSCRIPTIONS;
}
