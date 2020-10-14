package edu.utexas.tacc.tapis.notifications.lib;

import com.rabbitmq.client.ConnectionFactory;

import java.util.Map;

public class RabbitMQConnection {

    private static ConnectionFactory INSTANCE;
    private static Map<String, String> envs = System.getenv();

    public static synchronized ConnectionFactory getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ConnectionFactory();
            INSTANCE.setUsername(envs.getOrDefault("RABBITMQ_USERNAME", "dev"));
            INSTANCE.setPassword(envs.getOrDefault("RABBITMQ_PASSWORD", "dev"));
            INSTANCE.setVirtualHost(envs.getOrDefault("RABBITMQ_VHOST", "dev"));
            INSTANCE.useNio();
        }
        return INSTANCE;
    }


}
