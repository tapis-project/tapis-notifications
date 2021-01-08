package edu.utexas.tacc.tapis.notifications.lib.dao;

public class DAOStatements {

    //language=SQL
    public static final String CREATE_TOPIC =
        "INSERT INTO topics " +
            "(tenant_id, name, schema, description, owner) " +
            "values(?,?,to_json(?::json),?,?) " +
            "returning " +
            "uuid, " +
            "tenant_id, " +
            "description, " +
            "id, " +
            "name, " +
            "created, " +
            "owner, " +
            "schema::text as schema";


    //language=SQL
    public static final String CREATE_NOTIFICATION_MECHANISM =
        "INSERT INTO  notification_mechanisms (tenant_id, subscription_id, mechanism, target) " +
            "VALUES (?, ?, ?, ?) " +
            "RETURNING *; ";

    //language=SQL
    public static final String CREATE_SUBSCRIPTION =
        "INSERT INTO subscriptions " +
            "(tenant_id, topic_id, filters) " +
            "values(?, ?, to_json(?)) " +
            "returning " +
            "id, topic_id, tenant_id, uuid, filters::text, topic_id ";


    //language=SQL
    public static final String GET_SUBSCRIPTIONS_BY_TOPIC_UUID =
        "SELECT sub.*, nm.* from subscriptions as sub " +
            "join notification_mechanisms nm on sub.id = nm.subscription_id " +
            "join topics t on sub.topic_id = t.id " +
            "where t.uuid = ?; ";

    //language=SQL
    public static final String GET_SUBSCRIPTION_BY_UUID =
        "SELECT * from subscriptions as subs where subs.uuid = ? ;";

    //language=SQL
    public static final String GET_MECHANISMS_FOR_SUBSCRIPTION =
        "SELECT * from notification_mechanisms where subscription_id = ?";

}
