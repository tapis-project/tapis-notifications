package edu.utexas.tacc.tapis.notifications.lib.pojo;

import java.time.Instant;

public interface INotification {


    String getTenant();


    /**
     * get the timestamp of the creation time of the notification
     * @return
     */
    Instant getCreated();

    /**
     * username of the END USER who should recieve the message
     * @return
     */
    String getUsername();

    /**
     * Get the username of the user who created the notification. This will always be a
     * service account name I believe.
     * @return
     */
    String getCreator();

    /**
     * Get the actual body of the notification.
     * @return
     */
    String getBody();

    /**
     * Returns the level of the notification, should be INFO or ERROR at the moment. Useful for
     * front end developers to distinguish visually the severity of the notification.
     * @return
     */
    String getLevel();



}
