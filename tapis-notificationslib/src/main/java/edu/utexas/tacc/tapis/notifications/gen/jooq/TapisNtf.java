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

import java.util.Arrays;
import java.util.List;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class TapisNtf extends SchemaImpl {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>tapis_ntf</code>
     */
    public static final TapisNtf TAPIS_NTF = new TapisNtf();

    /**
     * The table <code>tapis_ntf.event_series</code>.
     */
    public final EventSeries EVENT_SERIES = EventSeries.EVENT_SERIES;

    /**
     * The table <code>tapis_ntf.flyway_schema_history</code>.
     */
    public final FlywaySchemaHistory FLYWAY_SCHEMA_HISTORY = FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY;

    /**
     * The table <code>tapis_ntf.notifications</code>.
     */
    public final Notifications NOTIFICATIONS = Notifications.NOTIFICATIONS;

    /**
     * The table <code>tapis_ntf.notifications_last_event</code>.
     */
    public final NotificationsLastEvent NOTIFICATIONS_LAST_EVENT = NotificationsLastEvent.NOTIFICATIONS_LAST_EVENT;

    /**
     * The table <code>tapis_ntf.notifications_recovery</code>.
     */
    public final NotificationsRecovery NOTIFICATIONS_RECOVERY = NotificationsRecovery.NOTIFICATIONS_RECOVERY;

    /**
     * The table <code>tapis_ntf.notifications_tests</code>.
     */
    public final NotificationsTests NOTIFICATIONS_TESTS = NotificationsTests.NOTIFICATIONS_TESTS;

    /**
     * The table <code>tapis_ntf.subscriptions</code>.
     */
    public final Subscriptions SUBSCRIPTIONS = Subscriptions.SUBSCRIPTIONS;

    /**
     * No further instances allowed
     */
    private TapisNtf() {
        super("tapis_ntf", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.asList(
            EventSeries.EVENT_SERIES,
            FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY,
            Notifications.NOTIFICATIONS,
            NotificationsLastEvent.NOTIFICATIONS_LAST_EVENT,
            NotificationsRecovery.NOTIFICATIONS_RECOVERY,
            NotificationsTests.NOTIFICATIONS_TESTS,
            Subscriptions.SUBSCRIPTIONS
        );
    }
}
