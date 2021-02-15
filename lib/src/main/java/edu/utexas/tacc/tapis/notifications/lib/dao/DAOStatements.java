package edu.utexas.tacc.tapis.notifications.lib.dao;

public class DAOStatements {

    //language=SQL
    public static final String CREATE_TOPIC =
        "INSERT INTO topics " +
            "(tenant_id, name, description, owner) " +
            "values(?,?,?,?) " +
            "returning " +
            "uuid, " +
            "tenant_id, " +
            "description, " +
            "id, " +
            "name, " +
            "created, " +
            "owner";
    //language=SQL
    public static final  String SEARCH_TOPICS =
        """
            
        """;


    //language=SQL
    public static final String CREATE_NOTIFICATION_MECHANISM =
        "INSERT INTO  notification_mechanisms (tenant_id, subscription_id, mechanism, target) " +
            "VALUES (?, ?, ?, ?) " +
            "RETURNING *; ";

    //language=SQL
    public static final String CREATE_SUBSCRIPTION =
        """
        INSERT INTO subscriptions 
        (tenant_id, topic_id, filters)
        values(?, ?, to_json(?))
        returning
        id, topic_id, tenant_id, uuid, filters::text
        """;

    //language=SQL
    public static final String DELETE_TOPIC_BY_UUID =
        """
        DELETE from topics where uuid = ?
        """;

    //language=SQL
    public static final String DELETE_TOPIC_BY_TENANT_TOPIC_NAME =
        """
        DELETE from topics where tenant_id = ? and name = ?;
        """;

    //language=SQL
    public static final String GET_SUBSCRIPTIONS_BY_TOPIC_UUID =
        """
        SELECT 
            sub.id as sub_id,
            sub.uuid as sub_uuid, 
            sub.tenant_id as sub_tenant_id,
            sub.topic_id as sub_topic_id,
            sub.filters::text as sub_filters,
            sub.created as sub_created,
            nm.id as nm_id, 
            nm.subscription_id as nm_subscription_id,
            nm.tenant_id as nm_tenant_id, 
            nm.subscription_id as nm_subscription_id, 
            nm.created as nm_created, 
            nm.uuid as nm_uuid, 
            nm.mechanism as nm_mechanism, 
            nm.target as nm_target                    
        from subscriptions as sub
        join notification_mechanisms nm on sub.id = nm.subscription_id
        join topics t on sub.topic_id = t.id 
        where t.uuid = ?;
        """;

    //language=SQL
    public static final String GET_TOPICS_FOR_OWNER_TENANT =
        """
        SELECT * from topics where tenant_id = ? and owner = ? 
        """;

    //language=SQL
    public static final String GET_TOPIC_BY_UUID =
        "SELECT * from topics where uuid = ? ;";

    //language=SQL
    public static final String GET_TOPIC_BY_NAME_AND_TENANT =
        """
        SELECT * from topics where tenant_id = ? and name = ?;
        """;

    //language=SQL
    public static final String GET_SUBSCRIPTION_BY_UUID =
        """
        SELECT * from subscriptions as subs where subs.uuid = ? ;
        """;

    //language=SQL
    public static final String GET_MECHANISMS_FOR_SUBSCRIPTION =
        """
        SELECT * from notification_mechanisms where subscription_id = ?
        """;

}
