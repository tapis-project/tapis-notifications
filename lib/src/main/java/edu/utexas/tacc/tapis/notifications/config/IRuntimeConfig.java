package edu.utexas.tacc.tapis.notifications.config;

public interface IRuntimeConfig {
    String getDbUsername();
    String getDbHost();
    String getDbName();
    String getDbPassword();
    String getDbPort();
    String getRabbitMQHost();
    String getRabbitMQUsername();
    String getRabbitMQVHost();
    String getRabbitmqPassword();
    String getServicePassword();
    String getTokensServiceURL();
    String getTenantsServiceURL();
    String getSiteId();
}