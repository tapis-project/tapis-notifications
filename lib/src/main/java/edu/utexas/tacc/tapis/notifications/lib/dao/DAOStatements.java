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
    public static final String CREATE_SUBSCRIPTION =
        "INSERT INTO subscriptions " +
            "(tenant_id, topic_id, filters) " +
            "values(?, ?, to_json(?)) " +
            "returning " +
            "id, topic_id, tenant_id, uuid, filters::text, topic_id ";


}
