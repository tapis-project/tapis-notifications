package edu.utexas.tacc.tapis.notifications;

import com.rabbitmq.client.ConnectionFactory;
import edu.utexas.tacc.tapis.notifications.config.IRuntimeConfig;
import edu.utexas.tacc.tapis.notifications.config.RuntimeSettings;

public class RabbitMQConnection {

    private static ConnectionFactory INSTANCE;

    public static synchronized ConnectionFactory getInstance() {
        if (INSTANCE == null) {
            IRuntimeConfig runtimeConfig = RuntimeSettings.get();
            INSTANCE = new ConnectionFactory();
            INSTANCE.setHost(runtimeConfig.getRabbitMQHost());
            INSTANCE.setUsername(runtimeConfig.getRabbitMQUsername());
            INSTANCE.setPassword(runtimeConfig.getRabbitmqPassword());
            INSTANCE.setVirtualHost(runtimeConfig.getRabbitMQVHost());
            INSTANCE.useNio();
        }
        return INSTANCE;
    }


}
