/*
 * This file is generated by jOOQ.
 */
package edu.utexas.tacc.tapis.notifications.gen.jooq;


import org.jooq.Sequence;
import org.jooq.impl.Internal;
import org.jooq.impl.SQLDataType;


/**
 * Convenience access to all sequences in tapis_notif.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Sequences {

    /**
     * The sequence <code>tapis_notif.subscriptions_seq_id_seq</code>
     */
    public static final Sequence<Integer> SUBSCRIPTIONS_SEQ_ID_SEQ = Internal.createSequence("subscriptions_seq_id_seq", TapisNotif.TAPIS_NOTIF, SQLDataType.INTEGER.nullable(false), null, null, null, null, false, null);
}