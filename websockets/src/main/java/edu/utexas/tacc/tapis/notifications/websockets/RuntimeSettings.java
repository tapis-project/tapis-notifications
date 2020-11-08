package edu.utexas.tacc.tapis.notifications.websockets;

import java.util.HashMap;
import java.util.Map;

public class RuntimeSettings {
    
    private static final Map<String, String> systemEnvs = System.getenv();

    private static final String rabbitmqHost = systemEnvs.getOrDefault("RABBITMQ_HOST", "localhost");
    private static final String rabbitmqVHost = systemEnvs.getOrDefault("RABBITMQ_VHOST", "dev");
    private static final String rabbitmqUser = systemEnvs.getOrDefault("RABBITMQ_USER", "dev");
    private static final String rabbitmqPassword = systemEnvs.getOrDefault("RABBITMQ_PASSWORD", "dev");

    public static String getRabbitmqHost() {
        return rabbitmqHost;
    }

    public static String getRabbitmqVHost() {
        return rabbitmqVHost;
    }

    public static String getRabbitmqUser() {
        return rabbitmqUser;
    }

    public static String getRabbitmqPassword() {
        return rabbitmqPassword;
    }
}
